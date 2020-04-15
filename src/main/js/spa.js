import React from 'react';

import http from './http';
import { Button, Badge, Row, Col, Container, Card, CardImg, CardText, CardBody, CardTitle, CardSubtitle, CardHeader, Input } from 'reactstrap';

import Dexie from 'dexie';

import db from './db';
import base64url from 'base64url';

function randomString() {
	return Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
}

function _base64ToArrayBuffer(base64) {
	var binary_string =  window.atob(base64);
	var len = binary_string.length;
	var bytes = new Uint8Array(len);
	for (var i = 0; i < len; i++)        {
		bytes[i] = binary_string.charCodeAt(i);
	}
	return bytes.buffer;
}

function _arrayBufferToBase64(buffer) {
    var binary = '';
    var bytes = new Uint8Array(buffer);
    var len = bytes.byteLength;
    for (var i = 0; i < len; i++) {
        binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary);
}

class SPA extends React.Component {
	
	alg = 'RS256'; // firefox doesn't like ES keys?
	
	txendpoint = 'http://host.docker.internal:9834/api/as/transaction';
	
	constructor(props) {
		super(props);
    
		this.state = {
				transactions: [],
				awaitingCallback: false,
				keypair: undefined,
				savedState: undefined
		};
	}
	
	componentDidMount = () => {
		db.savedState.toArray().then(dbs => {
			if (dbs.length === 1) {
				// get the only item
				console.log('Loading previous configuration state...');
				const savedState = dbs[0];
				this.setState({
					savedState: savedState.id,
					keypair: savedState.keypair,
					transactions: savedState.transactions,
					awaitingCallback: savedState.awaitingCallback,
					clientNonce: savedState.clientNonce,
					redir: savedState.redir,
					txHandle: savedState.txHandle,
					serverNonce: savedState.serverNonce				
				}, () => this.checkCallback());
			} else {
				// clear the storage just in case
				console.log('Generating a new configuration...');
				db.savedState.clear().then(() => {
					this.createKeys();
				}).catch(e => {
					console.error(e);
				});
			}
		});
	};

	saveState = () => {
		return db.savedState.clear().then(() => {
			db.savedState.put({
				awaitingCallback: this.state.awaitingCallback,
				keypair: this.state.keypair,
				transactions: this.state.transactions,
				clientNonce: this.state.clientNonce,
				redir: this.state.redir,
				txHandle: this.state.txHandle,
				serverNonce: this.state.serverNonce				
			}).then(id => {
				this.setState({
					savedState: id
				});
			}).catch(function(error) {
				  console.error(error);
			});
		});
	};
	
	createKeys = () => {
		return this.generateKeyPair().then(keypair => {
			
			/*
			crypto.subtle.exportKey('jwk', this.state.keypair.privateKey).then(jwk => {
				console.log(jwk);
			});
			*/
			
			this.setState({
				keypair: keypair
			}, () => this.saveState());
		});
	};
	
	// use an RSA signature
	keyParams = { name: 'RSASSA-PKCS1-V1_5', hash: { name: 'SHA-256' }, modulusLength: 2048, publicExponent: new Uint8Array([0x01, 0x00, 0x01]) };
	
	generateKeyPair = () => {
		return crypto.subtle.generateKey(this.keyParams, false, ['sign']);
	};
	
	fetchWithDJWS = (url, method, body) => {
		const h = {
			alg: 'RS256',
			b64: false,
			typ: 'JOSE',
			crit: ['b64']
		};
		
		const header = JSON.stringify(h);
		
		const base = base64url.encode(header)
			+ '.'
			+ body;
		
		//console.log(base);
		
		const encoder = new TextEncoder();
		
		return crypto.subtle.sign(this.keyParams, this.state.keypair.privateKey, encoder.encode(base)).then(sig => {
			
			//console.log(base64url.fromBase64(_arrayBufferToBase64(sig)));
			
			const jwsd = base64url.encode(header) + '..' + base64url.fromBase64(_arrayBufferToBase64(sig));
		
			//console.log(jwsd);
			//console.log(body);
		
			const headers = {
				'Detached-JWS': jwsd,
				'Content-Type': 'application/json'
			};
			
			return fetch(url, {
				method: method,
				headers: headers,
				body: body
			});
		});
	};
	
	newTransaction = (e) => {
		e.preventDefault();
		
		return crypto.subtle.exportKey('jwk', this.state.keypair.publicKey).then(jwk => {
			const nonce = randomString();
			const redir = randomString();
			
			const t = {
				resources: ['foo', 'bar'],
				keys: {
					proof: 'jwsd',
					jwk: jwk
				},
				interact: {
					redirect: true,
					callback: {
						uri: 'http://host.docker.internal:9834/spa/' + redir,
						nonce: nonce,
						hash_method: 'sha2'
					}
				}
			};
			
			const body = JSON.stringify(t);
				
			return this.fetchWithDJWS(this.txendpoint, 'POST', body).then(res => {
				if (res.ok) {
					res.json().then(data => {
						// process the tx response
						this.setState({
							clientNonce: nonce,
							redir: redir,
							txHandle: data.handle.value, // this assumes a bearer handle
							serverNonce: data.server_nonce,
							awaitingCallback: true
						}, () => {
							this.saveState().then(() => {
								// now that we've saved the state we can go to the interaction endpoint
								window.location.assign(data.interaction_url);
								
							});
						});
					});
				}
			});
		});
	};

	checkCallback = () => {
		if (this.state.awaitingCallback) {
			
			// first see if this is the callback we're expecting
			if (window.location.pathname.endsWith(this.state.redir)) {
				// get parameters
				const query = new URLSearchParams(window.location.search);

				const hash = query.get('hash');
				const interactRef = query.get('interact');
				
				// check the hash
				const hashbase = this.state.clientNonce + '\n'
					+ this.state.serverNonce + '\n'
					+ interactRef;
				
				const encoder = new TextEncoder();
				
				crypto.subtle.digest('SHA-512', encoder.encode(hashbase)).then(digest => {
					const expected = base64url.fromBase64(_arrayBufferToBase64(digest));
					if (hash === expected) {
						// hash matches, call the tx endpoint again to follow up
						const t = {
							handle: this.state.txHandle,
							interact_ref: interactRef
						};
						
						const body = JSON.stringify(t);
						
						return this.fetchWithDJWS(this.txendpoint, 'POST', body).then(res => {
							if (res.ok) {
								res.json().then(data => {
									// process the tx response
									this.setState({
										accessToken: data.access_token.value,
										txHandle: data.handle.value,
										redir: undefined,
										clientNonce: undefined,
										serverNonce: undefined
									}, () => {
										window.history.pushState({}, 'post-redirect', '/spa');
										this.saveState();
									});
								});
							}
						});
						
					} else {
						console.log('Hash did not match, got ' + hash + ' expected ' + expected);
					}
				});
				
			} else {
				console.log('Not the callback we are looking for');
			}
		}
	};
	
	poll = (e) => {
		e.preventDefault();
		
		const t = {
			handle: this.state.txHandle
		};
		
		const body = JSON.stringify(t);
		
		return this.fetchWithDJWS(this.txendpoint, 'POST', body).then(res => {
			if (res.ok) {
				res.json().then(data => {
					// process the tx response
					this.setState({
						accessToken: data.access_token.value,
						txHandle: data.handle.value
					});
				});
			}
		});

	}
	
	render() {
		return (
			<Container>
				<Button color="success" onClick={this.newTransaction}>New SPA Transaction</Button>
				<KeyStatus keypair={this.state.keypair} createKeys={this.createKeys} />
				<Card outline color="primary">
					<CardHeader>
						<Button color="info" onClick={this.poll}>Poll</Button>
					</CardHeader>
					<CardBody>
						<dl className="row">
							<dt className="col-sm-3">Client nonce</dt>
							<dd className="col-sm-9">{this.state.clientNonce}</dd>
							<dt className="col-sm-3">Server nonce</dt>
							<dd className="col-sm-9">{this.state.serverNonce}</dd>
							<dt className="col-sm-3">Redir URL key</dt>
							<dd className="col-sm-9">{this.state.redir}</dd>
							<dt className="col-sm-3">Handle</dt>
							<dd className="col-sm-9">{this.state.txHandle}</dd>
							<dt className="col-sm-3">Access Token</dt>
							<dd className="col-sm-9">{this.state.accessToken}</dd>
						</dl>
					</CardBody>
				</Card>
			</Container>
		);
	}
	
}


const KeyStatus = ({...props}) => {
	if (props.keypair) {
		return ( 
			<Button color="warning" onClick={props.createKeys}>Keys <Badge color="success">Loaded</Badge></Button>
		);
	} else {
		return ( 
			<Button color="warning" onClick={props.createKeys}>Keys <Badge color="danger">Loading...</Badge></Button>
		);
	}
}

export default SPA;
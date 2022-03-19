import React from 'react';

import http from './http';
import { Button, Badge, Row, Col, Container, Card, CardImg, CardText, CardBody, CardTitle, CardSubtitle, CardHeader, Input } from 'reactstrap';
import {RequestParameterForm, AccessToken, PendingTransactionEntry, PendingTransaction} from './client';
import Dexie from 'dexie';

import db from './db';

import base64 from 'base-64';

import { serializeDictionary, serializeItem, serializeList, ByteSequence } from 'structured-headers';

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
	
	constructor(props) {
		super(props);
    
		this.state = {
			transactions: [],
			instances: {},
			requestForm: {
				grantEndpoint: 'https://gnap-as.herokuapp.com/api/as/transaction',
				proof: 'httpsig',
				display: undefined,
				accessToken: `{
	  "access": [ "foo", "bar", "baz", {
	        "type": "photo-api",
	        "actions": [
	            "read",
	            "write",
	            "delete"
	        ],
	        "locations": [
	            "https://server.example.net/",
	            "https://resource.local/other"
	        ],
	        "datatypes": [
	            "metadata",
	            "images"
	        ]
	    }]
	}`,
				interactStart: [ 'redirect' ],
				interactFinish: true,
				user: undefined,
				subject: undefined,
				httpSigAlgorithm: undefined,
				digest: 'sha-512'
			},
			showForm: true,
			awaitingCallback: false,
			keypair: undefined,
			generatedPublicKeyJwk: undefined,
			savedState: undefined
		};
	}
	
	componentDidMount = () => {
		document.title = 'XYZ SPA Client';
		
		db.savedState.toArray().then(dbs => {
			if (dbs.length === 1) {
				// get the only item
				console.log('Loading previous configuration state...');
				const savedState = dbs[0];
				crypto.subtle.exportKey('jwk', savedState.keypair.publicKey).then(jwk => {
					this.setState({
						generatedPublicKeyJwk: JSON.stringify(jwk, null, 2)
					});
				});
				this.setState({
					savedState: savedState.id,
					keypair: savedState.keypair,
					requestForm: savedState.requestForm,
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
				requestForm: this.state.requestForm,
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

	showForm = (e) => {
		e.preventDefault();
		this.setState({
			showForm: true
		});
	}
	
		setGrantEndpoint = (e) => {
		var requestForm = {...this.state.requestForm};
		requestForm.grantEndpoint = e.target.value ? e.target.value : undefined;
		
		this.setState({
			requestForm: requestForm
		});
	}

	setPrivateKey = (e) => {
		// do nothing
	}

	setProof = (e) => {
		var requestForm = {...this.state.requestForm};
		requestForm.proof = e.target.value ? e.target.value : undefined;
		
		this.setState({
			requestForm: requestForm
		});
	}

	setDisplay = (e) => {
		var requestForm = {...this.state.requestForm};
		requestForm.display = e.target.value ? e.target.value : undefined;
		
		this.setState({
			requestForm: requestForm
		});
	}

	setAccessToken = (e) => {
		var requestForm = {...this.state.requestForm};
		requestForm.accessToken = e.target.value ? e.target.value : undefined;
		
		this.setState({
			requestForm: requestForm
		});
	}

	setInteractStart = (e) => {
		var opts = [];

		for (let i = 0; i < event.target.options.length; i++) {
			if (event.target.options[i].selected) {
				opts.push(event.target.options[i].value);
			}
		}
	
		var requestForm = {...this.state.requestForm};
		requestForm.interactStart = opts;
		
		this.setState({
			requestForm: requestForm
		});
	}

	setInteractFinish = (e) => {
		var requestForm = {...this.state.requestForm};
		requestForm.interactFinish = e.target.checked;
		
		this.setState({
			requestForm: requestForm
		});
	}

	setUser = (e) => {
		var requestForm = {...this.state.requestForm};
		requestForm.user = e.target.value ? e.target.value : undefined;
		
		this.setState({
			requestForm: requestForm
		});
	}

	setSubject = (e) => {
		var requestForm = {...this.state.requestForm};
		requestForm.subject = e.target.value ? e.target.value : undefined;
		
		this.setState({
			requestForm: requestForm
		});
	}

	setHttpSigAlgorithm = (e) => {
		var requestForm = {...this.state.requestForm};
		requestForm.httpSigAlgorithm = e.target.value ? e.target.value : undefined;
		
		this.setState({
			requestForm: requestForm
		});
	}
	
	setDigest = (e) => {
		var requestForm = {...this.state.requestForm};
		requestForm.digest = e.target.value ? e.target.value : undefined;
		
		this.setState({
			requestForm: requestForm
		});
	}
	
	createKeys = () => {
		var keyParams = { name: 'RSA-PSS', hash: { name: 'SHA-512' }, modulusLength: 2048, publicExponent: new Uint8Array([0x01, 0x00, 0x01]) };
		
		return crypto.subtle.generateKey(keyParams, false, ['sign']).then(keypair => {
			
			crypto.subtle.exportKey('jwk', keypair.publicKey).then(jwk => {
				this.setState({
					generatedPublicKeyJwk: JSON.stringify(jwk, null, 2)
				});
			});
			
			this.setState({
				keypair: keypair
			}, () => this.saveState());
		});
	};
	
	signedFetch = (uri, method, body, at) => {
	
		var headers = {};
	
		var coveredComponents = [
			'@method',
			'@target-uri'
		];

		if (at) {
			coveredComponents.push('authorization');
			headers['authorization'] = 'GNAP ' + at;
		}
	
		// create the body hash
		if (body) {
			const encoder = new TextEncoder();

			return crypto.subtle.digest('SHA-512', encoder.encode(body)).then(hashBuffer => {
				var bodyHash = _arrayBufferToBase64(hashBuffer);
				console.log(bodyHash);
				headers['content-digest'] = serializeDictionary(
					new Map([
						['sha-512', [new ByteSequence(bodyHash), new Map()]]
					])
				);
				coveredComponents.push('content-digest');
				
				headers['content-type'] = 'application/json';
				coveredComponents.push('content-type');
				
				return this.signedFetchInternal(uri, method, body, coveredComponents, headers);
			});
		} else {
			return this.signedFetchInternal(uri, method, body, coveredComponents, headers);
		}
	}
		
	signedFetchInternal = (uri, method, body, coveredComponents, headers) => {
		var sigBase = '';
		coveredComponents.forEach(c => {
			var componentValue = undefined;
			if (c.startsWith('@')) {
				// it's derived
				if (c === '@method') {
					componentValue = method;
				} else if (c === '@target-uri') {
					componentValue = uri;
				}
			} else {
				// it's a header
				componentValue = headers[c.toLowerCase()];
			}
			
			if (componentValue) {
				sigBase += serializeItem([c, new Map()]);
				sigBase += ': ';
				sigBase += componentValue;
				sigBase += '\n';
			}
		});
		
		var params = new Map();
		params.set('alg', 'rsa-pss-sha512');
		params.set('created', Math.floor(Date.now() / 1000));
		
		var sigParamsRaw = [coveredComponents.map(c => [c, new Map()]), params];
		var sigParams = serializeList([sigParamsRaw]);
		
		sigBase += serializeItem(['@signature-params', new Map()]);
		sigBase += ': ';
		sigBase += sigParams;
		
		console.log(sigBase);
		
		const encoder = new TextEncoder();
		return crypto.subtle.digest('SHA-512', encoder.encode(sigBase)).then(hashedBase => {
		
			var rsaPssParams = { name: 'RSA-PSS', saltLength: 64 };
		
			return crypto.subtle.sign(rsaPssParams, this.state.keypair.privateKey, hashedBase).then(signature => {
				var sigBytes = _arrayBufferToBase64(signature);
				
				headers['signature'] = serializeDictionary(
					new Map([
						['sig', [new ByteSequence(sigBytes), new Map()]]
					])
				);
				headers['signature-input'] = serializeDictionary(
					new Map([
						['sig', sigParamsRaw]
					])
				);
				console.log(headers);
				
				return fetch(uri, {
					method: method,
					body: body,
					headers: headers
				}).then(res => {
					return res.json();
				}).then(json => {
					console.log(json);
				});
				
			});
			
		});
		
		
	};
	
	newTransaction = (e) => {
		e.preventDefault();
		
		/*
		const data = {
			grant_endpoint: this.state.requestForm.grantEndpoint,
			private_key: this.state.requestForm.privateKey ? JSON.parse(this.state.requestForm.privateKey) : undefined,
			proof: this.state.requestForm.proof,
			display: this.state.requestForm.display ? JSON.parse(this.state.requestForm.display) : undefined,
			access_token: this.state.requestForm.accessToken ? JSON.parse(this.state.requestForm.accessToken) : undefined,
			interact_start: this.state.requestForm.interactStart,
			interact_finish: this.state.requestForm.interactFinish,
			user: this.state.requestForm.user ? JSON.parse(this.state.requestForm.user) : undefined,
			subject: this.state.requestForm.subject ? JSON.parse(this.state.requestForm.subject) : undefined,
			http_sig_algorithm: this.state.requestForm.httpSigAlgorithm,
			digest: this.state.requestForm.digest
		};
		*/
		
		const data = {
			access_token: this.state.requestForm.accessToken ? JSON.parse(this.state.requestForm.accessToken) : undefined,
			subject: this.state.requestForm.subject ? JSON.parse(this.state.requestForm.subject) : undefined,
			user: this.state.requestForm.user ? JSON.parse(this.state.requestForm.user) : undefined,
		};
		
		data['client'] = {
			key: {
				proof: this.state.requestForm.proof,
				jwk: JSON.parse(this.state.generatedPublicKeyJwk)
			},
			display: this.state.requestForm.display ? JSON.parse(this.state.requestForm.display) : undefined
		};
		
		const nonce = randomString();
		const redir = randomString();
		
		data['interact'] = {
			start: this.state.interactStart,
			finish: this.state.interactFinish ?
				{
					method: 'redirect',
					uri: 'http://localhost:9834/spa/' + redir,
					nonce: nonce
				} : undefined
		};
		
		console.log(data);
		
		this.signedFetch(this.state.requestForm.grantEndpoint,
			'POST',
			JSON.stringify(data),
			this.state.requestForm.httpSigAlgorithm,
			this.state.requestForm.digest
		);
		
		return;
		
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
				},
				display: {
					name: 'XYZ Single Page App',
					uri: 'https://oauth.xyz/'
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
				{ this.state.showForm &&
					<RequestParameterForm
						grantEndpoint={this.state.requestForm.grantEndpoint}
						setGrantEndpoint={this.setGrantEndpoint}
						privateKey={this.state.generatedPublicKeyJwk}
						setPrivateKey={this.setPrivateKey}
						privateKeyReadOnly={true}
						proof={this.state.requestForm.proof}
						setProof={this.setProof}
						httpSigAlgorithm={this.state.requestForm.httpSigAlgorithm}
						setHttpSigAlgorithm={this.setHttpSigAlgorithm}
						digest={this.state.requestForm.digest}
						setDigest={this.setDigest}
						display={this.state.requestForm.display}
						setDisplay={this.setDisplay}
						accessToken={this.state.requestForm.accessToken}
						setAccessToken={this.setAccessToken}
						interactStart={this.state.requestForm.interactStart}
						setInteractStart={this.setInteractStart}
						interactFinish={this.state.requestForm.interactFinish}
						setInteractFinish={this.setInteractFinish}
						user={this.state.requestForm.user}
						setUser={this.setUser}
						subject={this.state.requestForm.subject}
						setSubject={this.setSubject}
					/>
				}
				{ !this.state.showForm && 
				<Button color="dark" onClick={this.showForm}>Show Client Instance Parameter Form</Button>
				}
				<Button color="success" onClick={this.newTransaction}>New SPA Transaction</Button>
				<KeyStatus keypair={this.state.keypair} createKeys={this.createKeys} />
				<Card outline color="primary">
					<CardHeader>
						<Button color="info" onClick={this.poll}>Poll</Button>
					</CardHeader>
					<CardBody>
						<dl className="row">
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
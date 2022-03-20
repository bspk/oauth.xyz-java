//Interaction Endpoint

import React from 'react';
import ReactDOM from 'react-dom';
import http from './http';
import { Button, Badge, Row, Col, Container, Card, CardImg, CardText, CardBody, CardTitle, CardSubtitle, CardHeader, CardFooter, Input } from 'reactstrap';
import { BrowserRouter, Switch, Route } from 'react-router-dom';


class Interact extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
				requireCode: props.requireCode,
				redirectTo: null,
				pending: null,
				rtn: false,
				userCode: undefined
		};

	}

	setUserCode = (e) => {
		var userCode = e.target.value;
		
		if (!userCode) {
			userCode = '';
		}
		
		userCode = userCode.replace(/l/, '1'); // lowercase ell is a one
		userCode = userCode.toUpperCase(); // shift everything to uppercase
		userCode = userCode.replace(/0/, 'O'); // oh is zero
		userCode = userCode.replace(/I/, '1'); // aye is one
		userCode = userCode.replaceAll(/[^123456789ABCDEFGHJKLMNOPQRSTUVWXYZ]/g, ""); // throw out all invalid characters

		if (userCode) {
			if (userCode.length > 4) {
				userCode = userCode.slice(0, 4) + ' - ' + userCode.slice(4, 8);
			}
		} else {
			userCode = undefined;
		}
		this.setState({
			userCode: userCode
		});
	}

	submit = () => {

		var data = {user_code: this.state.userCode};

		var _self = this;

		$.ajax({
			url: '/api/as/interact/device',
			type: 'POST',
			contentType: 'application/json',
			data: JSON.stringify(data),
			success: function(data, status) {
				_self.setState({requireCode:false}, () => {
					// it was submitted sucessfully, load the approval page
					_self.loadPending();
				});
			},
			error: function(jqxhr, status, error) {
				// there was an error
			}
		});
	}

	componentDidMount() {
		document.title = "XYZ Interaction";

		this.loadPending();
	}

	loadPending = () => {
		return http({
			method: 'GET',
			path: '/api/as/interact/pending'
		}).done(
				response => {
					this.setState({
						pending: response.entity,
						redirectTo: null,
						rtn: false
					});
				}, 
				error => {
					this.setState({
						redirectTo: null,
						pending: null,
						rtn: false
					});
				}
		);
	}

	approve = () => {
		var data = {approved: true};

		this.postApproval(data);
	}

	postApproval = (data) => {

		var _self = this;

		$.ajax({
			url: '/api/as/interact/approve',
			type: 'POST',
			contentType: 'application/json',
			data: JSON.stringify(data),
			success: response => {
				if (response.uri) {
					// follow the redirect
					_self.setState({
						redirectTo: response.uri,
						pending: null,
						rtn: false
					});
				} else if (response.approved) {
					_self.setState({
						redirectTo: null,
						pending: null,
						rtn: true
					});
				}
			},
			error: function(jqxhr, status, error) {
				// there was an error
			}
		});
	}

	deny = () => {
		var data = {approved: false};

		this.postApproval(data);
	}

	render() {
		console.log(this.state);
		if (this.state.redirectTo) {
			return (
					<Redirect uri={this.state.redirectTo} />
			);
		} else if (this.state.rtn) {
			return (
					<div>Please return to your device.</div>
			);
		} else if (this.state.requireCode) {
			return (
					<UserCodeForm submit={this.submit} setUserCode={this.setUserCode} userCode={this.state.userCode} />
			);
		} else if (this.state.pending && this.state.pending.transaction) {
			return (
					<ApprovalForm pending={this.state.pending} approve={this.approve} deny={this.deny} />
			);
		} else {
			return (
					<div>There are no pending transactions, go away</div>
			);
		}
	}
}

class UserCodeForm extends React.Component {
	render() {
		return (

				<Card body>
				<Input type="text" id="userCode" placeholder="XXXX - XXXX" onChange={this.props.setUserCode} value={this.props.userCode ? this.props.userCode : ''}/>
					<Button color="success" onClick={this.props.submit}>Submit</Button>
					</Card>

		);
	}
}

class ApprovalForm extends React.Component {
	render() {
		return (

				<Card>

				<CardBody>
				<ClientInfo display={this.props.pending.transaction.display} />
				<AccessRequestInfo access={this.props.pending.transaction.access_token_request} />
				<SubjectRequestInfo subject={this.props.pending.transaction.subject_request} />
				</CardBody>
				<CardFooter>
				<Button color="success" onClick={this.props.approve}>Approve</Button>
				<Button color="deny" onClick={this.props.deny}>Deny</Button>
				</CardFooter>
				</Card>

		);
	}
}

class ClientInfo extends React.Component {
	render() {

		if (this.props.display) {
			return (
	<div>
		<h2>{this.props.display.name || "Client"}</h2>
		<span>{this.props.display.uri}</span>
	</div>
			);
		} else {
			return(
				<div>
					<h2>"Client"</h2>
				</div>
			);
		}


	}
}

class AccessRequestInfo extends React.Component {
	render() {

		if (this.props.access) {
		
			if (Array.isArray(this.props.access)) {
				// multiple token request
				const access = this.props.access.map(mt => {
					
					if (mt.access) {
						const st = mt.access.map(a => {
							if (typeof a === 'string' || a instanceof String) {
								// it's a reference
								return (
									<li><i>{a}</i></li>
								);
							} else {
								// it's an object, display the type
								console.log(a);
								return (
									<li><b>{a.type}</b></li>
								);
							}
						
						});
						return <li>Token {mt.label}:<ul>{st}</ul></li>;
					} else {
						return null;
					}
				});
				return (
						<div>
						Access:
						<ul>{access}</ul>
						</div>
				);
			} else {
				// single token request
				if (this.props.access.access) {
					const access = this.props.access.access.map(a => {
						if (typeof a === 'string' || a instanceof String) {
							// it's a reference
							return (
								<li><i>{a}</i></li>
							);
						} else {
							// it's an object, display the type
							console.log(a);
							return (
								<li><b>{a.type}</b></li>
							);
						}
					
					});
					return (
							<div>
							Access:
							<ul>{access}</ul>
							</div>
					);
				} else {
					return null;
				}
			}
		} else {
			return null;
		}


	}
}

class SubjectRequestInfo extends React.Component {
	render() {

		if (this.props.subject) {
			if (this.props.subject.sub_id_formats) {
				const subj = this.props.subject.sub_id_formats.map(a => {
					return (
						<li><i>{a}</i></li>
					);
				});
				return (
						<div>
						Subject Identifiers:
						<ul>{subj}</ul>
						</div>
				);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
}


class Redirect extends React.Component {
	constructor( props ){
		super();
		this.state = { ...props };
	}
	componentWillMount(){
		window.location = this.state.uri;
	}
	render(){
		return (<section>Redirecting...</section>);
	}
}


export default Interact;
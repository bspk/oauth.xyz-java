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
				redirectTo: null,
				pending: null,
				rtn: false
		};

	}

	submit = () => {

		var userCode = $('#userCode').val();

		var data = {user_code: userCode};

		var _self = this;

		$.ajax({
			url: '/api/as/interact/device',
			type: 'POST',
			contentType: 'application/json',
			data: JSON.stringify(data),
			success: function(data, status) {
				// it was submitted sucessfully, load the approval page
				_self.loadPending();
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
		} else if (this.state.pending && this.state.pending.require_code) {
			return (
					<UserCodeForm submit={this.submit} />
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
				<Input type="text" id="userCode" placeholder="XXXX - XXXX" />
					<Button colo="success" onClick={this.props.submit}>Submit</Button>
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
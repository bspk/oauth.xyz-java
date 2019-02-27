// Interaction Endpoint

import React from 'react';
import ReactDOM from 'react-dom';
import http from './http';
import { Button, Badge, Row, Col, Container, Card, CardImg, CardText, CardBody, CardTitle, CardSubtitle, CardHeader, Input } from 'reactstrap';
import { BrowserRouter, Switch, Route } from 'react-router-dom';


class Interact extends React.Component {
	constructor(props) {
		super(props);
		
		this.submit = this.submit.bind(this);
		this.loadPending = this.loadPending.bind(this);
		
		this.state = {
			pending: null
		};
		
	}
	
	submit() {

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
		document.title = "OAuth.XYZ Interaction";
		
		this.loadPending();
	}
	
	loadPending() {
		return http({
			method: 'GET',
			path: '/api/as/interact/pending'
		}).done(
			response => {
				this.setState({
					pending: response.entity
				});
			}, 
			error => {
				this.setState({
					pending: null
				});
			}
		);
	}
	
	render() {
		if (this.state.pending && this.state.pending.require_code) {
			return (
				<UserCodeForm submit={this.submit} />
			);
		} else if (this.state.pending && this.state.pending.transaction) {
			return (
				<div>Your transaction has been submitted, return to your device</div>
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
		
		<Card body>
		
			<Button color="success" onClick={this.approve}>Approve</Button>
			<Button color="deny" onClick={this.deny}>Deny</Button>
		
		</Card>
		
	}
}

export default Interact;
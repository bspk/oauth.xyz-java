// Client page

import React from 'react';
import ReactDOM from 'react-dom';
import http from './http';
import { Button, Badge, Row, Col, Container, Card, CardImg, CardText, CardBody, CardTitle, CardSubtitle, CardHeader, Input } from 'reactstrap';
import { FaClone } from 'react-icons/fa';
import { BrowserRouter, Switch, Route } from 'react-router-dom';
import QRCode from 'qrcode.react';

class Client extends React.Component {
	constructor(props) {
		super(props);
		
		this.state = {
			transactions: [],
			instances: {}
		};
	}
	
	componentDidMount = () => {
		document.title = "XYZ Client";
		
		this.loadPending();
	}
	
	newTransaction = (e) => {
		http({
			method: 'POST',
			path: '/api/client/authcode'
		}).done(response => {
			this.loadPending();
		});
	}
	
	newDevice = (e) => {
		http({
			method: 'POST',
			path: '/api/client/device'
		}).done(response => {
			this.loadPending();
		});
	}
	
	newScannable = (e) => {
		http({
			method: 'POST',
			path: '/api/client/scannable'
		}).done(response => {
			this.loadPending();
		});
	}
	
	clearInstanceIds = (e) => {
		http({
			method: 'DELETE',
			path: '/api/client/ids'
		}).done(response => {
			this.setState({
				instances: {}
			});
		});
	}
	
	getInstanceIds = () => {
		http({
			method: 'GET',
			path: '/api/client/ids'
		}).done(response => {
			this.setState({
				instances: response.entity
			});
		});
	}
	
	loadPending = () => {
		console.log('Getting pending transactions...');
		http({
			method: 'GET',
			path: '/api/client/pending'
		}).done(response => {
			this.setState({
				transactions: response.entity
			});
		});
		this.getInstanceIds();
	}
	
	cancel = (transactionId) => () => {
		http({
			method: 'DELETE',
			path: '/api/client/poll/' + encodeURIComponent(transactionId)
		}).done(response => {
			this.loadPending();
		});
	}

	poll = (transactionId) => () => {
		http({
			method: 'POST',
			path: '/api/client/poll/' + encodeURIComponent(transactionId)
		}).done(response => {
			this.loadPending();
		});
	}
	
	use = (transactionId) => () => {
		http({
			method: 'POST',
			path: '/api/client/use/' + encodeURIComponent(transactionId)
		}).done(response => {
			this.loadPending();
		});
	}


	render() {
		
		const pending = this.state.transactions.map(
				transaction => (
					<PendingTransaction key={transaction.id} transaction={transaction} cancel={this.cancel} poll={this.poll} use={this.use} />
				)
			).reverse(); // newest first
	
		return (
			<Container>
				<Button color="success" onClick={this.newTransaction}>New Auth Code Transaction <InstanceBadge instance={this.state.instances['authCodeId']} /></Button>
				<Button color="warning" onClick={this.newDevice}>New Device Transaction <InstanceBadge instance={this.state.instances['deviceId']} /></Button>
				<Button color="dark" onClick={this.newScannable}>New Scannable Transaction <InstanceBadge instance={this.state.instances['scannableId']} /></Button>
				<Button color="danger" size="sm" onClick={this.clearInstanceIds}>Clear Instance Ids</Button>
				<Button color="primary" size="sm" onClick={this.loadPending}>Refresh</Button>
				{pending}
			</Container>
		);
	}
	
}

class InstanceBadge extends React.Component{
	render() {
		if (this.props.instance) {
			return (<Badge title={this.props.instance}><FaClone/></Badge>);
		} else {
			return null;
		}
	}
}

class PendingTransaction extends React.Component{
	render() {
		const controls = [];
		
		return (
			<Card outline color="primary">
				<CardHeader>
					<Button color="info" onClick={this.props.poll(this.props.transaction.id)}>Poll</Button>
					<Button color="danger" onClick={this.props.cancel(this.props.transaction.id)}>Cancel</Button>
					<Button color="success" onClick={this.props.use(this.props.transaction.id)}>Use</Button>
				</CardHeader>
				<CardBody>
					<PendingTransactionEntry transaction={this.props.transaction} />
				</CardBody>
			</Card>
		);
	}
}

class PendingTransactionEntry extends React.Component {
	render() {
		const elements = [];
		
		if (this.props.transaction.access_token) {
			elements.push(
				...[
					<dt className="col-sm-3">Token</dt>,
					<dd className="col-sm-9"><AccessToken token={this.props.transaction.access_token} /></dd>
				]
			);
		}
		
		if (this.props.transaction.multiple_access_tokens) {
			const tokens = Object.keys(this.props.transaction.multiple_access_tokens)
			.filter(k => this.props.transaction.multiple_access_tokens[k])
			.map(k => (
				<>
					<b>{k}</b>: <AccessToken token={this.props.transaction.multiple_access_tokens[k]} />
					<br/>
				</>
			));
			//debugger;
			elements.push(
				...[
					<dt className="col-sm-3">Multiple Tokens</dt>,
					<dd className="col-sm-9">{tokens}</dd>
				]
			);
		}

		/*
		if (this.props.entry.response.claims) {
			const claims = Object.keys(this.props.entry.response.claims)
			.filter(k => this.props.entry.response.claims[k])
			.map(k => (
				<>
					<b>{k}</b>: <em>{this.props.entry.response.claims[k]}</em>
					<br/>
				</>
			));
			//debugger;
			elements.push(
				...[
					<dt className="col-sm-3">Claims</dt>,
					<dd className="col-sm-9">{claims}</dd>
				]
			);
		}
		*/
		
		if (this.props.transaction.continue_token) {
			elements.push(
				...[
					<dt className="col-sm-3">Continuation Token</dt>,
					<dd className="col-sm-9">{this.props.transaction.continue_token}</dd>
				]
			);
		}
		
		if (this.props.transaction.user_code) {
			elements.push(
				...[
					<dt className="col-sm-3">User Code URL</dt>,
					<dd className="col-sm-9"><a href={this.props.transaction.user_code_url}>{this.props.transaction.user_code_url}</a></dd>,
					<dt className="col-sm-3">User Code</dt>,
					<dd className="col-sm-9"><UserCode userCode={this.props.transaction.user_code} /></dd>
				]
			);
		}
		if (this.props.transaction.interaction_url) {
			elements.push(
				...[
					<dt className="col-sm-3">Interaction URL</dt>,
					<dd className="col-sm-9"><a href={this.props.transaction.interaction_url}>{this.props.transaction.interaction_url}</a></dd>
				]
			);
		}
		
		if (this.props.transaction.interaction_url && this.props.transaction.user_code) {
			elements.push(
				...[
					<dt className="col-sm-3">Scannable Interaction URL</dt>,
					<dd className="col-sm-9"><QRCode value={this.props.transaction.interaction_url} /></dd>
				]
			);
		}
		
		return (
			<Card body className={this.props.last ? null : "bg-light text-muted"}>
				<dl className="row">
					{elements}
				</dl>
			</Card>
		);		
	}
}

class AccessToken extends React.Component {
	render() {
		
		if (this.props.token) {
			return (
				<Badge color="info" pill>{this.props.token}</Badge>
			);
		} else {
			return null;
		}
		
	}
}

class UserCode extends React.Component {
	render() {
		
		if (this.props.userCode) {
			return (
				<Badge color="info" pill>{this.props.userCode.slice(0, 4)} - {this.props.userCode.slice(4)}</Badge>
			);
		} else {
			return null;
		}
		
	}
}





export default Client;

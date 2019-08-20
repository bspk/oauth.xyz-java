// Client page

import React from 'react';
import ReactDOM from 'react-dom';
import http from './http';
import { Button, Badge, Row, Col, Container, Card, CardImg, CardText, CardBody, CardTitle, CardSubtitle, CardHeader, Input } from 'reactstrap';
import { BrowserRouter, Switch, Route } from 'react-router-dom';
import { QRCode } from 'qrcode.react';

class Client extends React.Component {
	constructor(props) {
		super(props);
		
		this.state = {
			transactions: []
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
	
	loadPending = () => {
		
		http({
			method: 'GET',
			path: '/api/client/pending'
		}).done(response => {
			this.setState({
				transactions: response.entity
			});
		});
	}
	
	render() {
		
		const pending = this.state.transactions.map(
				transaction => (
					<PendingTransaction key={transaction.id} transaction={transaction} />
				)
			).reverse(); // newest first
	
		return (
			<Container>
				<Button color="success" onClick={this.newTransaction}>New Auth Code Transaction</Button>
				<Button color="warning" onClick={this.newDevice}>New Device Transaction</Button>
				{pending}
			</Container>
		);
	}
	
}

class PendingTransaction extends React.Component{
	constructor(props) {
		super(props);
		
		this.state = {
			transaction: props.transaction
		};
	}
	
	poll = () => {
		http({
			method: 'POST',
			path: '/api/client/poll/' + encodeURIComponent(this.state.transaction.id)
		}).done(response => {
			this.setState({
				transaction: response.entity
			});
		});
	}

	render() {
		const controls = [];
		
		return (
			<Card outline color="primary">
				<CardHeader>
					<Button color="info" onClick={this.poll}>Poll</Button>
				</CardHeader>
				<CardBody>
					<PendingTransactionEntryList transaction={this.state.transaction} />
				</CardBody>
			</Card>
		);
	}
}

class PendingTransactionEntryList extends React.Component{
	
	render() {
		const entries = this.props.transaction.entries.map(
			(entry, i, arr) => (
				<PendingTransactionEntry key={entry.id} entry={entry} last={i === arr.length - 1} />
			)
		).reverse();
		
		return (
			<>
				{entries}
			</>
		);
		
	}

}

class PendingTransactionEntry extends React.Component {
	render() {
		if (!this.props.last) {
			return null;
		}

		const elements = [];
		
		if (this.props.entry.response.access_token) {
			elements.push(
				...[
					<dt className="col-sm-3">Token</dt>,
					<dd className="col-sm-9"><AccessToken token={this.props.entry.response.access_token} /></dd>
				]
			);
		}
		
		if (this.props.entry.response.handle) {
			elements.push(
				...[
					<dt className="col-sm-3">Transaction Handle</dt>,
					<dd className="col-sm-9">{this.props.entry.response.handle.value}</dd>
				]
			);
		}
		
		if (this.props.entry.response.user_code) {
			elements.push(
				...[
					<dt className="col-sm-3">User Code URL</dt>,
					<dd className="col-sm-9"><a href={this.props.entry.response.user_code_url}>{this.props.entry.response.user_code_url}</a></dd>,
					<dt className="col-sm-3">User Code</dt>,
					<dd className="col-sm-9"><UserCode userCode={this.props.entry.response.user_code} /></dd>
				]
			);
		}
		if (this.props.entry.response.interaction_url) {
			elements.push(
				...[
					<dt className="col-sm-3">Interaction URL</dt>,
					<dd className="col-sm-9"><a href={this.props.entry.response.interaction_url}>{this.props.entry.response.interaction_url}</a></dd>
				]
			);
		}
		
		if (this.props.entry.response.user_code && this.props.entry.response.interaction_url) {
			elements.push(
				...[
					<dt className="col-sm-3">Interaction URL</dt>,
					<dd className="col-sm-9"><QRCode value={this.props.entry.response.interaction_url} /></dd>
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
				<Badge color="info" pill>{this.props.token.value}</Badge>
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

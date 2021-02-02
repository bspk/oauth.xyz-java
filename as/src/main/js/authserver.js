// Authorization server admin page

import React from 'react';
import ReactDOM from 'react-dom';
import http from './http';
import { Button, Badge, Row, Col, Container, Card, CardImg, CardText, CardBody, CardTitle, CardSubtitle, CardHeader, Input } from 'reactstrap';
import { BrowserRouter, Switch, Route } from 'react-router-dom';


class AuthServer extends React.Component {
	
	constructor(props) {
		super(props);
		
		this.state = {
			transactions: [],
			clients: []
		};
	}
	
	componentDidMount() {
		document.title = "XYZ Auth Server";
		this.refreshTransactions();
		this.refreshClients();
	}
	
	refreshTransactions = () => {
		return http({
			method: 'GET',
			path: '/api/transaction/getall'
		}).done(response => {
			this.setState({
				transactions: response.entity
			});
		});	
	}
	
	cancelTransaction = (transactionId) => (e) => {
		http({
			method: 'DELETE',
			path: '/api/transaction/' + encodeURIComponent(transactionId)
		}).done(response => {
			this.refreshTransactions();
		});
	
	}
	
	refreshClients = () => {
		return http({
			method: 'GET',
			path: '/api/clients'
		}).done(response => {
			this.setState({
				clients: response.entity._embedded.clients
			});
		});	
	}
	
	cancelClient = (clientId) => (e) => {
		http({
			method: 'DELETE',
			path: clientId
		}).done(response => {
			this.refreshClients();
		});
	
	}
	
	render() {
		return (
			<Container>
				<Button color="primary" onClick={this.refreshTransactions}>Refresh Transactions</Button>
				<Button color="secondary" onClick={this.refreshClients}>Refresh Clients</Button>
				<TransactionList transactions={this.state.transactions} cancelTransaction={this.cancelTransaction} />
				<ClientList clients={this.state.clients} cancelClient={this.cancelClient} />
			</Container>
		);
	}

}

class TransactionList extends React.Component{
	render() {
		const transactions = this.props.transactions.map(
			transaction =>
				<Transaction key={transaction.id} transaction={transaction} cancelTransaction={this.props.cancelTransaction} />
		).reverse(); // newest first
		return (
			<>
				<h3>Transactions</h3>
				{transactions}
			</>
		);
		
	}
}

class Transaction extends React.Component{
	render() {
		return (
			<Card>
				<CardHeader>
					<Button color="danger" onClick={this.props.cancelTransaction(this.props.transaction.id)} >Cancel</Button>
				</CardHeader>
				<CardBody>
				<dl className="row">
					<dt className="col-sm-3">Status</dt>
					<dd className="col-sm-9"><TransactionStatus status={this.props.transaction.status} /></dd>
					{this.props.transaction.continue_access_token &&
					<>
						<dt className="col-sm-3">Continue Token</dt>
						<dd className="col-sm-9">{this.props.transaction.continue_access_token.value}</dd>
					</>
					}
					{this.props.transaction.interact &&
					<>
						<dt className="col-sm-3">Interaction URL</dt>
						<dd className="col-sm-9"><a href={this.props.transaction.interact.interaction_url}>{this.props.transaction.interact.interaction_url}</a></dd>
						<dt className="col-sm-3">User Code</dt>
						<dd className="col-sm-9">{this.props.transaction.interact.user_code}</dd>
						<dt className="col-sm-3">User Code URL</dt>
						<dd className="col-sm-9"><a href={this.props.transaction.interact.user_code_url}>{this.props.transaction.interact.user_code_url}</a></dd>
						<dt className="col-sm-3">Callback URL</dt>
						<dd className="col-sm-9"><a href={this.props.transaction.interact.callback_uri}>{this.props.transaction.interact.callback_uri}</a></dd>
						<dt className="col-sm-3">Callback Method</dt>
						<dd className="col-sm-9">{this.props.transaction.interact.callback_method}</dd>
						<dt className="col-sm-3">Callback Hash Method</dt>
						<dd className="col-sm-9">{this.props.transaction.interact.callback_hash_method}</dd>
						<dt className="col-sm-3">Server Nonce</dt>
						<dd className="col-sm-9">{this.props.transaction.interact.server_nonce}</dd>
						<dt className="col-sm-3">Client Nonce</dt>
						<dd className="col-sm-9">{this.props.transaction.interact.client_nonce}</dd>
					</>
					}
				</dl>
				</CardBody>
			</Card>
		);
	}
}

class TransactionStatus extends React.Component {
	render() {
		switch (this.props.status) {
			case 'new':
				return (<Badge color="info">{this.props.status}</Badge>);
			case 'issued':
				return (<Badge color="success">{this.props.status}</Badge>);
			case 'authorized':
				return (<Badge color="warning">{this.props.status}</Badge>);
			case 'waiting':
				return (<Badge color="dark">{this.props.status}</Badge>);
			case 'denied':
				return (<Badge color="danger">{this.props.status}</Badge>);
			default:
				return (<Badge color="primary">UNKNOWN : {this.props.status}</Badge>);
		}
	}
}

class ClientList extends React.Component{
	render() {
		const clients = this.props.clients.map(
			client =>
				<Client key={client._links.self.href} client={client} cancelClient={this.props.cancelClient} />
		).reverse(); // newest first
		return (
			<>
				<h3>Clients</h3>
				{clients}
			</>
		);
		
	}
}

class Client extends React.Component{
	render() {
		return (
			<Card>
				<CardHeader>
					<Button color="danger" onClick={this.props.cancelClient(this.props.client._links.self.href)} >Cancel</Button>
				</CardHeader>
				<CardBody>
				<dl className="row">
					<dt className="col-sm-3">ID</dt>
					<dd className="col-sm-9">{this.props.client._links.self.href}</dd>
					{this.props.client.display &&
					<>
						<dt className="col-sm-3">Display Name</dt>
						<dd className="col-sm-9">{this.props.client.display.name}</dd>
						<dt className="col-sm-3">Homepage</dt>
						<dd className="col-sm-9"><a href={this.props.client.display.uri}>{this.props.client.display.uri}</a></dd>
					</>
					}
					{this.props.client.key &&
					<>
						<dt className="col-sm-3">Proof</dt>
						<dd className="col-sm-9">{this.props.client.key.proof}</dd>
					</>
					}
				</dl>
				</CardBody>
			</Card>
		);
	}
}



export default AuthServer;

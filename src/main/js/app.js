import React from 'react';
import ReactDOM from 'react-dom';
import http from './http';
import { Button, Badge, Row, Col, Container, Card, CardImg, CardText, CardBody, CardTitle, CardSubtitle } from 'reactstrap';
import { BrowserRouter, Switch, Route } from 'react-router-dom';

class AuthServer extends React.Component {
	
	constructor(props) {
		super(props);
		
		this.state = {
			transactions: []
		};
	}
	
	componentDidMount() {
		http({
			method: 'GET',
			path: '/api/transactions'
		}).done(response => {
			this.setState({
				transactions: response.entity._embedded.transactions
			});
		});
	}
	
	render() {
		return (
			<div>
				<TransactionList transactions={this.state.transactions} />
			</div>
		);
	}

}

class LoggingButton extends React.Component {
	// This syntax ensures `this` is bound within handleClick.
	// Warning: this is *experimental* syntax.
	handleClick(e) {
		console.log('this is:', this);
	}

	render() {
		return (
			<Button color="danger" onClick={(e) => this.handleClick(e)}>
				Click me
			</Button>
		);
	}
}

class TransactionList extends React.Component{
	render() {
		const transactions = this.props.transactions.map(
			transaction =>
				<Transaction key={transaction._links.self.href} transaction={transaction} />
		);
		return (
			<Container>
				<h3>Transactions</h3>
				{transactions}
			</Container>
		);
		
	}
}

class Transaction extends React.Component{
	render() {
		return (
			<Card body>
				<dl className="row">
					<dt className="col-sm-3">State</dt>
					<dd className="col-sm-9"><TransactionState state={this.props.transaction.state} /></dd>
					<dt className="col-sm-3">Transaction Handle</dt>
					<dd className="col-sm-9">{this.props.transaction.handles.transaction.value}</dd>
					{this.props.transaction.interact && this.props.transaction.interact.url &&
					<React.Fragment>
						<dt className="col-sm-3">Interaction</dt>
						<dd className="col-sm-9"><a href={this.props.transaction.interact.url}>{this.props.transaction.interact.url}</a></dd>
					</React.Fragment>
					}
					<dt className="col-sm-3">Interact</dt>
				</dl>
			</Card>
		);
	}
}

class TransactionState extends React.Component {
	render() {
		switch (this.props.state) {
			case 'new':
				return <Badge color="info">{this.props.state}</Badge>
			case 'issued':
				return <span className="badge badge-success">{this.props.state}</span>
			case 'authorized':
				return <span className="badge badge-warning">{this.props.state}</span>
			case 'waiting':
				return <span className="badge badge-dark">{this.props.state}</span>
			default:
				return null;
		}
	}
}

class Client extends React.Component {
	constructor(props) {
		super(props);
		
		this.state = {
			transactions: []
		};
		
		this.loadPending = this.loadPending.bind(this);
		this.newTransaction = this.newTransaction.bind(this);
	}
	
	componentDidMount() {
		this.loadPending();
	}
	
	newTransaction(e) {
		http({
			method: 'POST',
			path: '/api/client/authcode'
		}).done(response => {
			this.loadPending();
		});
	}
	
	loadPending() {
		
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
			);
	
		return (
			<div>
				<Button color="success" onClick={this.newTransaction}>New Transaction</Button>
				{pending}
			</div>
		);
	}
	
}

class PendingTransaction extends React.Component{
	render() {
		return (
			<PendingTransactionEntryList transaction={this.props.transaction} />
		);
	}
}

class PendingTransactionEntryList extends React.Component{

	render() {
		const entries = this.props.transaction.entries.map(
			entry => (
				<PendingTransactionEntry key={entry.id} entry={entry} />
			)
		);
		
		return (
			<div>
				<Button color="info" onClick={this.poll}>Poll</Button>
				{entries}
			</div>
		);
		
	}
	
}

class PendingTransactionEntry extends React.Component {
	render() {
		return (
			<Card body>
				<dl className="row">
					<dt className="col-sm-3">Token</dt>
					<dd className="col-sm-9"><AccessToken token={this.props.entry.response.access_token} /></dd>
					<dt className="col-sm-3">Transaction Handle</dt>
					<dd className="col-sm-9">{this.props.entry.response.handles.transaction.value}</dd>
					<dt className="col-sm-3">Interaction</dt>
					<dd className="col-sm-9"><a href={this.props.entry.response.interaction_url}>{this.props.entry.response.interaction_url}</a></dd>
					<dt className="col-sm-3">Interact</dt>
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

ReactDOM.render((
	<BrowserRouter>
		<Switch>
			<Route path='/as' component={AuthServer} />
			<Route path='/c' component={Client} />
		</Switch>
	</BrowserRouter>
	),
	document.getElementById('react')
);

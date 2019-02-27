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
			transactions: []
		};
	}
	
	componentDidMount() {
		document.title = "XYZ Auth Server";
		
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
		).reverse(); // newest first
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
					<dt className="col-sm-3">Status</dt>
					<dd className="col-sm-9"><TransactionStatus status={this.props.transaction.status} /></dd>
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
				return (<Badge color="error">{this.props.status}</Badge>);
			default:
				return (<Badge color="primary">UNKNOWN</Badge>);
		}
	}
}

export default AuthServer;

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
			default:
				return (<Badge color="error">UNKNOWN</Badge>);
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
		this.newDevice = this.newDevice.bind(this);
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
	
	newDevice(e) {
		http({
			method: 'POST',
			path: '/api/client/device'
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
				<Button color="success" onClick={this.newTransaction}>New Auth Code Transaction</Button>
				<Button color="warning" onClick={this.newDevice}>New Device Transaction</Button>
				{pending}
			</div>
		);
	}
	
}

class PendingTransaction extends React.Component{
	constructor(props) {
		super(props);
		
		this.poll = this.poll.bind(this);
		
		this.state = {
			transaction: props.transaction
		};
	}
	
	poll() {
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
			<React.Fragment>
				{entries}
			</React.Fragment>
		);
		
	}

}

class PendingTransactionEntry extends React.Component {
	render() {
		if (!this.props.last) {
			return null;
		}
		
		return (
			<Card body className={this.props.last ? null : "bg-light text-muted"}>
				<dl className="row">
					<dt className="col-sm-3">Token</dt>
					<dd className="col-sm-9"><AccessToken token={this.props.entry.response.access_token} /></dd>
					<dt className="col-sm-3">Transaction Handle</dt>
					<dd className="col-sm-9">{this.props.entry.response.handles.transaction.value}</dd>
					<dt className="col-sm-3">Interaction</dt>
					<dd className="col-sm-9"><a href={this.props.entry.response.interaction_url}>{this.props.entry.response.interaction_url}</a></dd>
					<dt className="col-sm-3">User Code</dt>
					<dd className="col-sm-9"><UserCode userCode={this.props.entry.response.user_code} /></dd>
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
				<Input type="text" id="userCode" placeholder="XXXX-XXXX" />
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

ReactDOM.render((
	<BrowserRouter>
		<Switch>
			<Route path='/as/interact' component={Interact} />
			<Route path='/as' component={AuthServer} />
			<Route path='/c' component={Client} />
		</Switch>
	</BrowserRouter>
	),
	document.getElementById('react')
);

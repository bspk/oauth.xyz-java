const React = require('react');
const ReactDOM = require('react-dom');
const client = require('./client');

class App extends React.Component {
	
	constructor(props) {
		super(props);
		
		this.state = {
			transactions: []
		};
	}
	
	componentDidMount() {
		client({
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
			<TransactionList transactions={this.state.transactions} />
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
			<div className="card">
				<div className="card-body">
					<h3 className="card-title">Transactions</h3>
					{transactions}
				</div>
			</div>
		);
		
	}
}

class Transaction extends React.Component{
	render() {
		return (
			<div className="card">
				<div className="card-body">
					<dl className="row">
						<dt className="col-sm-3">State</dt>
						<dd className="col-sm-9"><TransactionState state={this.props.transaction.state} /></dd>
						<dt className="col-sm-3">Transaction Handle</dt>
						<dd className="col-sm-9">{this.props.transaction.handles.transaction.value}</dd>
						{this.props.transaction.interact && this.props.transaction.interact.url &&
						<React.Fragment>
							<dt className="col-sm-3">Interaction</dt>
							<dd className="col-sm-9"><a href="{this.props.transaction.interact.url}">{this.props.transaction.interact.url}</a></dd>
						</React.Fragment>
						}
						<dt className="col-sm-3">Interact</dt>
					</dl>
				</div>
			</div>
			
		);
	}
}

class TransactionState extends React.Component {
	render() {
		switch (this.props.state) {
			case 'new':
				return <span className="badge badge-info">{this.props.state}</span>
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

ReactDOM.render(
	<App />,
	document.getElementById('react')
);

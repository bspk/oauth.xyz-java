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
			<ul>
				{transactions}
			</ul>
		);
		
	}
}

class Transaction extends React.Component{
	render() {
		return (
			<li>{this.props.transaction.state} / {this.props.transaction.handles.transaction.value}</li>
		);
	}
}

ReactDOM.render(
	<App />,
	document.getElementById('react')
);

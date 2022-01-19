import React from 'react';

import ReactDOM from 'react-dom';
import http from './http';
import { Button, Badge, Row, Col, Container, Card, CardImg, CardText, CardBody, CardTitle, CardSubtitle, CardHeader, Input } from 'reactstrap';
import { BrowserRouter, Switch, Route } from 'react-router-dom';

import AuthServer from './authserver';
import Interact from './interact';

class RootPage extends React.Component {
	
	constructor(props) {
		super(props);
		
		this.state = {
			rootUrl: "/api/as/transaction"
		};
	}
	
	componentDidMount() {
		document.title = "XYZ Authorization Server";
		this.loadRootUrl();
	}
	
	loadRootUrl = () => {
		return http({
			method: 'GET',
			path: '/api/whoami'
		}).done(response => {
			this.setState({
				rootUrl: response.entity.rootUrl + "api/as/transaction"
			});
		});	
	}
	render() {
		return (
			<Card outline color="warning">
				<CardHeader>
					Authorization Server
				</CardHeader>
				<CardBody>
					<p>GNAP Endpoint: <code>{this.state.rootUrl}</code></p>
				</CardBody>
			</Card>
		);
	}

}

ReactDOM.render((
	<BrowserRouter>
		<Switch>
			<Route path='/device'>
 				{
 					() => <Interact requireCode={true} />
 				}
 			</Route>
			<Route path='/interact' component={Interact} />
			<Route path='/as' component={AuthServer} />
			<Route path='/' component={RootPage} />
		</Switch>
	</BrowserRouter>
	),
	document.getElementById('react')
);

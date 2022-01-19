import React from 'react';

import ReactDOM from 'react-dom';
import http from './http';
import { Button, Badge, Row, Col, Container, Card, CardImg, CardText, CardBody, CardTitle, CardSubtitle, CardHeader, Input } from 'reactstrap';
import { BrowserRouter, Switch, Route } from 'react-router-dom';


class RootPage extends React.Component {
	
	constructor(props) {
		super(props);
		
		this.state = {
			rootUrl: "/api/rs"
		};
	}
	
	componentDidMount() {
		document.title = "XYZ Resource Server";
		this.loadRootUrl();
	}
	
	loadRootUrl = () => {
		return http({
			method: 'GET',
			path: '/api/whoami'
		}).done(response => {
			this.setState({
				rootUrl: response.entity.rootUrl + "api/rs"
			});
		});	
	}
	render() {
		return (
			<Card outline color="warning">
				<CardHeader>
					Resource Server
				</CardHeader>
				<CardBody>
					<p>To access the resource send a GET request to <code>{this.state.rootUrl}</code> with an access token and associated key.</p>
				</CardBody>
			</Card>
		);
	}

}



ReactDOM.render((
		<RootPage />
	),
	document.getElementById('react')
);

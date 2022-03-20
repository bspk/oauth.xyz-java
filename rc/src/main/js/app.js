import React from 'react';

import ReactDOM from 'react-dom';
import http from './http';
import { Button, Badge, Row, Col, Container, Card, CardImg, CardText, CardBody, CardTitle, CardSubtitle, CardHeader, Input } from 'reactstrap';
import { BrowserRouter, Switch, Route } from 'react-router-dom';

import Client from './client';
import SPA from './spa';

ReactDOM.render((
	<BrowserRouter>
		<Switch>
			<Route path='/spa' component={SPA} />
			<Route path='/' component={Client} />
		</Switch>
	</BrowserRouter>
	),
	document.getElementById('react')
);

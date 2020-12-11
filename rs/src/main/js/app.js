import React from 'react';

import ReactDOM from 'react-dom';
import http from './http';
import { Button, Badge, Row, Col, Container, Card, CardImg, CardText, CardBody, CardTitle, CardSubtitle, CardHeader, Input } from 'reactstrap';
import { BrowserRouter, Switch, Route } from 'react-router-dom';





ReactDOM.render((
			<Card outline color="warning">
				<CardHeader>
					Resource Server
				</CardHeader>
				<CardBody>
					<p>To access the resource send a GET request to /api/rs with an access token and associated key.</p>
				</CardBody>
			</Card>
	),
	document.getElementById('react')
);

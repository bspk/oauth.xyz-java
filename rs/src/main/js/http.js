'use strict';

const rest = require('rest');
const defaultRequest = require('rest/interceptor/defaultRequest');
const mime = require('rest/interceptor/mime');
const uriTemplateInterceptor = require('./api/uriTemplateInterceptor');
const errorCode = require('rest/interceptor/errorCode');
const baseRegistry = require('rest/mime/registry');

const registry = baseRegistry.child();

registry.register('text/uri-list', require('./api/uriListConverter'));
registry.register('application/hal+json', require('rest/mime/type/application/hal'));
registry.register('application/json', require('rest/mime/type/application/json'));

module.exports = rest
		.wrap(mime, { registry: registry })
		.wrap(uriTemplateInterceptor)
		.wrap(errorCode)
		.wrap(defaultRequest, { headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' }});
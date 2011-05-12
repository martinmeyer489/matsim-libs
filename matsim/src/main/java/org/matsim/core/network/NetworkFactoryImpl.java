/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008, 2011 by the members listed in the COPYING,  *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.network;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.GenericRouteFactory;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.RouteFactory;

/**
 * @author dgrether
 * @author mrieser
 */
public class NetworkFactoryImpl implements NetworkFactory {

	private final static Logger log = Logger.getLogger(NetworkFactoryImpl.class);

	private LinkFactory linkFactory = null;

	private final Map<String, RouteFactory> routeFactories = new HashMap<String, RouteFactory>();
	private RouteFactory defaultFactory = new GenericRouteFactory();

	private final NetworkImpl network;

	public NetworkFactoryImpl(final NetworkImpl network) {
		this.network = network;
		this.linkFactory = new LinkFactoryImpl();
		this.routeFactories.put(TransportMode.car, new LinkNetworkRouteFactory());
		this.routeFactories.put(TransportMode.ride, new LinkNetworkRouteFactory());
		this.routeFactories.put(TransportMode.pt, new GenericRouteFactory());
	}

	@Override
	public NodeImpl createNode(final Id id, final Coord coord) {
		NodeImpl node = new NodeImpl(id);
		node.setCoord(coord) ;
		return node ;
	}

	/*
	 * TODO how to set other attributes of link consistently without invalidating time variant attributes
	 */
	@Override
	public Link createLink(Id id, Node fromNode, Node toNode) {
		return this.linkFactory.createLink(id, fromNode, toNode, this.network, 1.0, 1.0, 1.0, 1.0);
	}

	@Override
	@Deprecated
	public Link createLink(final Id id, final Id fromNodeId, final Id toNodeId) {
		Node fromNode = this.network.getNodes().get(fromNodeId) ;
		if ( fromNode==null ) {
			log.error("could not find fromNodeId in network; this will probably fail downstream; have you added the node to the network?") ;
		}
		Node toNode = this.network.getNodes().get(toNodeId) ;
		if ( toNode==null ) {
			log.error("could not find toNodeId in network; this will probably fail downstream; have you added the node to the network?") ;
		}
		return createLink(id, fromNode, toNode);
	}

	public Link createLink(final Id id, final Node from, final Node to,
			final NetworkImpl network, final double length, final double freespeedTT, final double capacity,
			final double lanes) {
		return this.linkFactory.createLink(id, from, to, network, length, freespeedTT, capacity, lanes);
	}

	/**
	 * @param transportMode the transport mode the route should be for
	 * @param startLink the link where the route starts
	 * @param endLink the link where the route ends
	 * @return a new Route for the specified mode
	 *
	 * @see #setRouteFactory(String, RouteFactory)
	 */
	public Route createRoute(final String transportMode, final Id startLinkId, final Id endLinkId) {
		RouteFactory factory = this.routeFactories.get(transportMode);
		if (factory == null) {
			factory = this.defaultFactory;
		}
		return factory.createRoute(startLinkId, endLinkId);
	}

	/**
	 * Registers a {@link RouteFactory} for the specified mode. If <code>factory</code> is <code>null</code>,
	 * the existing entry for this <code>mode</code> will be deleted. If <code>mode</code> is <code>null</code>,
	 * then the default factory is set that is used if no specific RouteFactory for a mode is set.
	 *
	 * @param transportMode
	 * @param factory
	 */
	public void setRouteFactory(final String transportMode, final RouteFactory factory) {
		if (transportMode == null) {
			this.defaultFactory = factory;
		} else {
			if (factory == null) {
				this.routeFactories.remove(transportMode);
			} else {
				this.routeFactories.put(transportMode, factory);
			}
		}
	}

	public void setLinkFactory(final LinkFactory factory) {
		this.linkFactory = factory;
	}

	public boolean isTimeVariant() {
		return (this.linkFactory instanceof TimeVariantLinkFactory);
	}

}

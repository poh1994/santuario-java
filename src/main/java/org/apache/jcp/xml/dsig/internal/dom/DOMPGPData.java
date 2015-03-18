/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/*
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 */
/*
 * $Id$
 */
package org.apache.jcp.xml.dsig.internal.dom;

import java.util.*;

import javax.xml.crypto.*;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.keyinfo.PGPData;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.utils.Base64;

/**
 * DOM-based implementation of PGPData.
 *
 * @author Sean Mullan
 */
public final class DOMPGPData extends BaseStructure implements PGPData {

    private final byte[] keyId;
    private final byte[] keyPacket;
    private final List<XMLStructure> externalElements;

    /**
     * Creates a <code>DOMPGPData</code> containing the specified key packet.
     * and optional list of external elements.
     *
     * @param keyPacket a PGP Key Material Packet as defined in section 5.5 of
     *    <a href="http://www.ietf.org/rfc/rfc2440.txt"/>RFC 2440</a>. The
     *    array is cloned to prevent subsequent modification.
     * @param other a list of {@link XMLStructure}s representing elements from
     *    an external namespace. The list is defensively copied to prevent
     *    subsequent modification. May be <code>null</code> or empty.
     * @throws NullPointerException if <code>keyPacket</code> is
     *    <code>null</code>
     * @throws IllegalArgumentException if the key packet is not in the
     *    correct format
     * @throws ClassCastException if <code>other</code> contains any
     *    entries that are not of type {@link XMLStructure}
     */
    public DOMPGPData(byte[] keyPacket, List<? extends XMLStructure> other) {
        if (keyPacket == null) {
            throw new NullPointerException("keyPacket cannot be null");
        }
        if (other == null || other.isEmpty()) {
            this.externalElements = Collections.emptyList();
        } else {
            this.externalElements =
                Collections.unmodifiableList(new ArrayList<XMLStructure>(other));
            for (int i = 0, size = this.externalElements.size(); i < size; i++) {
                if (!(this.externalElements.get(i) instanceof XMLStructure)) {
                    throw new ClassCastException
                        ("other["+i+"] is not a valid PGPData type");
                }
            }
        }
        this.keyPacket = keyPacket.clone();
        checkKeyPacket(keyPacket);
        this.keyId = null;
    }

    /**
     * Creates a <code>DOMPGPData</code> containing the specified key id and
     * optional key packet and list of external elements.
     *
     * @param keyId a PGP public key id as defined in section 11.2 of
     *    <a href="http://www.ietf.org/rfc/rfc2440.txt"/>RFC 2440</a>. The
     *    array is cloned to prevent subsequent modification.
     * @param keyPacket a PGP Key Material Packet as defined in section 5.5 of
     *    <a href="http://www.ietf.org/rfc/rfc2440.txt"/>RFC 2440</a> (may
     *    be <code>null</code>). The array is cloned to prevent subsequent
     *    modification.
     * @param other a list of {@link XMLStructure}s representing elements from
     *    an external namespace. The list is defensively copied to prevent
     *    subsequent modification. May be <code>null</code> or empty.
     * @throws NullPointerException if <code>keyId</code> is <code>null</code>
     * @throws IllegalArgumentException if the key id or packet is not in the
     *    correct format
     * @throws ClassCastException if <code>other</code> contains any
     *    entries that are not of type {@link XMLStructure}
     */
    public DOMPGPData(byte[] keyId, byte[] keyPacket,
                      List<? extends XMLStructure> other)
    {
        if (keyId == null) {
            throw new NullPointerException("keyId cannot be null");
        }
        // key ids must be 8 bytes
        if (keyId.length != 8) {
            throw new IllegalArgumentException("keyId must be 8 bytes long");
        }
        if (other == null || other.isEmpty()) {
            this.externalElements = Collections.emptyList();
        } else {
            this.externalElements =
                Collections.unmodifiableList(new ArrayList<XMLStructure>(other));
            for (int i = 0, size = this.externalElements.size(); i < size; i++) {
                if (!(this.externalElements.get(i) instanceof XMLStructure)) {
                    throw new ClassCastException
                        ("other["+i+"] is not a valid PGPData type");
                }
            }
        }
        this.keyId = keyId.clone();
        this.keyPacket = keyPacket == null ? null
                                           : keyPacket.clone();
        if (keyPacket != null) {
            checkKeyPacket(keyPacket);
        }
    }

    /**
     * Creates a <code>DOMPGPData</code> from an element.
     *
     * @param pdElem a PGPData element
     */
    public DOMPGPData(Element pdElem) throws MarshalException {
        // get all children nodes
        byte[] keyId = null;
        byte[] keyPacket = null;
        
        List<XMLStructure> other = new ArrayList<XMLStructure>();
        Node firstChild = pdElem.getFirstChild();
        while (firstChild != null) {
            if (firstChild.getNodeType() == Node.ELEMENT_NODE) {
                Element childElem = (Element)firstChild;
                String localName = childElem.getLocalName();
                String namespace = childElem.getNamespaceURI();
                try {
                    if (localName.equals("PGPKeyID") && XMLSignature.XMLNS.equals(namespace)) {
                        keyId = Base64.decode(childElem);
                    } else if (localName.equals("PGPKeyPacket") && XMLSignature.XMLNS.equals(namespace)) {
                        keyPacket = Base64.decode(childElem);
                    } else {
                        other.add
                            (new javax.xml.crypto.dom.DOMStructure(childElem));
                    }
                } catch (Base64DecodingException bde) {
                    throw new MarshalException(bde);
                }
            }
            firstChild = firstChild.getNextSibling();
        }
        this.keyId = keyId;
        this.keyPacket = keyPacket;
        this.externalElements = Collections.unmodifiableList(other);
    }

    @Override
    public byte[] getKeyId() {
        return keyId == null ? null : keyId.clone();
    }

    @Override
    public byte[] getKeyPacket() {
        return keyPacket == null ? null : keyPacket.clone();
    }

    @Override
    public List<XMLStructure> getExternalElements() {
        return externalElements;
    }

    /**
     * We assume packets use the new format packet syntax, as specified in
     * section 4 of RFC 2440.
     *
     * This method only checks if the packet contains a valid tag. The
     * contents of the packet should be checked by the application.
     */
    private void checkKeyPacket(byte[] keyPacket) {
        // length must be at least 3 (one byte for tag, one byte for length,
        // and minimally one byte of content
        if (keyPacket.length < 3) {
            throw new IllegalArgumentException("keypacket must be at least " +
                                               "3 bytes long");
        }

        int tag = keyPacket[0];
        // first bit must be set
        if ((tag & 128) != 128) {
            throw new IllegalArgumentException("keypacket tag is invalid: " +
                                               "bit 7 is not set");
        }
        // make sure using new format
        if ((tag & 64) != 64) {
            throw new IllegalArgumentException("old keypacket tag format is " +
                                               "unsupported");
        }

        // tag value must be 6, 14, 5 or 7
        if ((tag & 6) != 6 && (tag & 14) != 14 && 
            (tag & 5) != 5 && (tag & 7) != 7) {
            throw new IllegalArgumentException("keypacket tag is invalid: " +
                                               "must be 6, 14, 5, or 7");
        }
    }
}

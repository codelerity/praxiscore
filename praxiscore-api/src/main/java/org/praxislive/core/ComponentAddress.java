/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2023 Neil C Smith.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 * 
 * 
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.praxislive.core.types.PMap;

/**
 * Address of a Component. A component is a slash separated path of IDs,
 * starting with the ID of the Root that the Component is in, eg.
 * {@code /rootID/parentID/componentID}. ComponentAddresses are always absolute.
 */
public final class ComponentAddress extends Value {

    /**
     * Value type name.
     */
    public static final String TYPE_NAME = "ComponentAddress";

    private static final String ADDRESS_REGEX = "\\G/([_\\-\\p{javaLetter}][_\\-\\p{javaLetterOrDigit}]*)";
    private static final String ID_REGEX = "[_\\-\\p{javaLetter}][_\\-\\p{javaLetterOrDigit}]*";
    private static final Pattern ID_PATTERN = Pattern.compile(ID_REGEX);
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(ADDRESS_REGEX);

    private final String[] address;
    private final String addressString;

    private ComponentAddress(String[] address, String addressString) {
        this.address = address;
        this.addressString = addressString;
    }

    /**
     * Number of ID parts to this address
     *
     * @return int Depth (always >=1)
     */
    public int depth() {
        return address.length;
    }

    /**
     * Get ID at given depth in address.
     *
     * @param depth
     * @return String ID
     */
    public String componentID(int depth) {
        return address[depth];
    }

    /**
     * Equivalent to componentID(depth() - 1).
     *
     * @return String
     */
    public String componentID() {
        return address[address.length - 1];
    }

    /**
     * Equivalent to componentID(0).
     *
     * @return String
     */
    public String rootID() {
        return address[0];
    }

    /**
     * The parent address. Returns null if this is a root address (depth == 1).
     *
     * @return parent address, or null if root address.
     */
    public ComponentAddress parent() {
        if (address.length == 1) {
            return null;
        } else {
            String s = addressString;
            s = s.substring(0, s.lastIndexOf('/'));
            s = cache(s);
            String[] a = Arrays.copyOfRange(address, 0, address.length - 1);
            return new ComponentAddress(a, s);
        }
    }

    /**
     * Resolve the provided path or child ID against this address. The path
     * should be relative and not start with a slash.
     *
     * @param path relative address to resolve
     * @return resolved address
     */
    public ComponentAddress resolve(String path) {
        return ComponentAddress.of(this, path);
    }

    /**
     * Get a {@link ControlAddress} for a control on this component.
     *
     * @param id control id
     * @return control address
     */
    public ControlAddress control(String id) {
        return ControlAddress.of(this, id);
    }

    /**
     * Get a {@link PortAddress} for a port on this component.
     *
     * @param id port id
     * @return port address
     */
    public PortAddress port(String id) {
        return PortAddress.of(this, id);
    }

    @Override
    public String toString() {
        return this.addressString;
    }

    @Override
    public int hashCode() {
        return this.addressString.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ComponentAddress) {
            ComponentAddress o = (ComponentAddress) obj;
            return addressString.equals(o.addressString);
        } else {
            return false;
        }
    }

    /**
     * Create an address from the supplied String
     *
     * @param addressString
     * @return ComponentAddress
     * @throws org.praxislive.core.ValueFormatException
     */
    public static ComponentAddress parse(String addressString) throws ValueFormatException {

        String[] address = parseAddress(addressString);
        return new ComponentAddress(address, cache(addressString));

    }

    /**
     * Create an address from the supplied String
     *
     * @param address
     * @return ComponentAddress
     * @throws IllegalArgumentException on invalid string
     */
    public static ComponentAddress of(String address) {
        try {
            return parse(address);
        } catch (ValueFormatException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Create a ComponentAddress by adding the supplied path to the end of the
     * supplied ComponentAddress.
     *
     * @param address
     * @param path
     * @return ComponentAddress
     * @throws IllegalArgumentException
     */
    public static ComponentAddress of(ComponentAddress address, String path) {
        try {
            return parse(address.toString() + '/' + path);
        } catch (ValueFormatException ex) {
            throw new IllegalArgumentException(ex);
        }

    }

    private static ComponentAddress coerce(Value arg) throws ValueFormatException {
        if (arg instanceof ComponentAddress) {
            return (ComponentAddress) arg;
        } else {
            return parse(arg.toString());
        }
    }

    public static Optional<ComponentAddress> from(Value arg) {
        try {
            return Optional.of(coerce(arg));
        } catch (ValueFormatException ex) {
            return Optional.empty();
        }
    }

    /**
     *
     * @param id
     * @return
     */
    public static boolean isValidID(String id) {
        return ID_PATTERN.matcher(id).matches();
    }

    private static String[] parseAddress(String addressString) throws ValueFormatException {
        Matcher match = ADDRESS_PATTERN.matcher(addressString);
        ArrayList<String> addressList = new ArrayList<String>();
        int end = 0;
        while (match.find()) {
            String id = match.group(1);
            addressList.add(cache(id));
            end = match.end();
        }
        if (addressList.size() < 1 || end < addressString.length()) {
            throw new ValueFormatException();
        }
        return addressList.toArray(new String[addressList.size()]);

    }

    public static ArgumentInfo info() {
        return ArgumentInfo.of(ComponentAddress.class, PMap.EMPTY);
    }

    static String cache(String string) {
        return string;
    }
}

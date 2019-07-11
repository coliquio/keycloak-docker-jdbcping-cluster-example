package de.coliquio.keycloak;

import org.infinispan.persistence.keymappers.DefaultTwoWayKey2StringMapper;
import org.jboss.logging.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

public class ExtendedKeyMapper extends DefaultTwoWayKey2StringMapper {
    private final static Logger logger = Logger.getLogger(ExtendedKeyMapper.class);
    private static final char NON_STRING_PREFIX = '\uFEFF';

    @Override
    public String getStringMapping(Object key) {
        logger.info("getStringMapping(" + key.toString() + "<" + key.getClass().toString() + ">)");
        if (key.getClass().equals(UUID.class)) {
            return NON_STRING_PREFIX + "u" + ((UUID) key).toString();
        }

        return super.getStringMapping(key);
    }

    @Override
    public Object getKeyMapping(String key) {
        logger.info("getKeyMapping(" + key + ")");

        if (key.length() > 0 && key.charAt(0) == NON_STRING_PREFIX && key.charAt(1) == 'u') {
            return UUID.fromString(key.substring(2));
        }

        return super.getKeyMapping(key);
    }

    @Override
    public boolean isSupportedType(Class<?> keyType) {
        return keyType == UUID.class || super.isSupportedType(keyType);
    }
}

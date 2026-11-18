package io.github.keel.iam.interfaces.admin.dto;

import io.github.keel.iam.application.identity.UserStatus;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/** Missing status preserves v1's ENABLED default; explicit null remains invalid. */
public class InitialUserStatusDeserializer extends ValueDeserializer<UserStatus> {
    @Override
    public UserStatus deserialize(JsonParser parser, DeserializationContext context) {
        if (parser.currentToken() != JsonToken.VALUE_STRING) {
            return context.reportInputMismatch(UserStatus.class, "status 必须为 ENABLED 或 DISABLED");
        }
        try {
            return UserStatus.valueOf(parser.getString());
        } catch (IllegalArgumentException ex) {
            return context.reportInputMismatch(UserStatus.class, "status 必须为 ENABLED 或 DISABLED");
        }
    }

    @Override
    public UserStatus getAbsentValue(DeserializationContext context) {
        return UserStatus.ENABLED;
    }

    @Override
    public UserStatus getNullValue(DeserializationContext context) {
        return context.reportInputMismatch(UserStatus.class, "status 不能为 null");
    }
}

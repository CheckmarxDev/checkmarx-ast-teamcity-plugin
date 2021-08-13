package com.checkmarx.teamcity.common;

import jetbrains.buildServer.serverSide.crypt.EncryptUtil;
import org.apache.commons.lang3.StringUtils;

import java.security.InvalidParameterException;
import java.util.Map;

import static com.checkmarx.teamcity.common.CheckmarxParams.*;

public class PluginUtils {

    public static String encrypt(String password) throws RuntimeException {
        String encPassword = "";
        if (!EncryptUtil.isScrambled(password)) {
            encPassword = EncryptUtil.scramble(password);
        } else {
            encPassword = password;
        }
        return encPassword;
    }

    public static String decrypt(String password) throws RuntimeException {
        String encStr = "";
        if (StringUtils.isNotEmpty(password)) {
            if (EncryptUtil.isScrambled(password)) {
                encStr = EncryptUtil.unscramble(password);
            } else {
                encStr = password;
            }
        }
        return encStr;
    }

    public static CheckmarxScanConfig resolveConfiguration(Map<String, String> runnerParameters, Map<String, String> sharedConfigParameters) {
        CheckmarxScanConfig scanConfig = new CheckmarxScanConfig();

        if (TRUE.equals(runnerParameters.get(USE_DEFAULT_SERVER))) {
            scanConfig.setServerUrl(validateNotEmpty(sharedConfigParameters.get(GLOBAL_AST_SERVER_URL), GLOBAL_AST_SERVER_URL));
            scanConfig.setClientId(validateNotEmpty(sharedConfigParameters.get(GLOBAL_AST_CLIENT_ID), GLOBAL_AST_CLIENT_ID));
            scanConfig.setAstSecret(decrypt(validateNotEmpty(sharedConfigParameters.get(GLOBAL_AST_SECRET), GLOBAL_AST_SECRET)));
            scanConfig.setAuthenticationUrl((sharedConfigParameters.get(GLOBAL_AST_AUTHENTICATION_URL)));
            scanConfig.setTenant((sharedConfigParameters.get(GLOBAL_AST_TENANT)));
        } else {
            scanConfig.setServerUrl(validateNotEmpty(runnerParameters.get(SERVER_URL), SERVER_URL));
            scanConfig.setClientId(validateNotEmpty(runnerParameters.get(AST_CLIENT_ID), AST_CLIENT_ID));
            scanConfig.setAstSecret(decrypt(validateNotEmpty(runnerParameters.get(AST_SECRET), AST_SECRET)));
            scanConfig.setAuthenticationUrl((runnerParameters.get(AUTHENTICATION_URL)));
            scanConfig.setTenant((runnerParameters.get(TENANT)));
        }

        if (TRUE.equals(runnerParameters.get(USE_GLOBAL_FILE_FILTERS))) {
            scanConfig.setZipFileFilters((sharedConfigParameters.get(GLOBAL_ZIP_FILTERS)));
        } else {
            scanConfig.setZipFileFilters((runnerParameters.get(ZIP_FILE_FILTERS)));
        }

        scanConfig.setProjectName(validateNotEmpty(runnerParameters.get(PROJECT_NAME), PROJECT_NAME));
        scanConfig.setAdditionalParameters(runnerParameters.get(ADDITIONAL_PARAMETERS));

        return scanConfig;

    }

    private static String validateNotEmpty(String param, String paramName) throws InvalidParameterException {
        if (param == null || param.length() == 0) {
            throw new InvalidParameterException("Parameter [" + paramName + "] must not be empty");
        }
        return param;
    }


}

package com.checkmarx.teamcity.agent.commands;


import com.checkmarx.teamcity.common.CheckmarxScanConfig;
import com.checkmarx.teamcity.common.PluginUtils;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import jetbrains.buildServer.agent.runner.SimpleProgramCommandLine;
import jetbrains.buildServer.util.PropertiesUtil;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.checkmarx.teamcity.common.CheckmarxParams.*;
import static java.util.Arrays.asList;
import static jetbrains.buildServer.util.StringUtil.nullIfEmpty;

public class CheckmarxScanCommand extends CheckmarxBuildServiceAdapter {

    private static final Logger LOG = Logger.getLogger(CheckmarxScanCommand.class);
    private static CheckmarxScanConfig scanConfig;

    @NotNull
    @Override
    public ProgramCommandLine makeProgramCommandLine() throws RunBuildException {

        //reference https://codingsight.com/implementing-a-teamcity-plugin/
        AgentRunningBuild agentRunningBuild = getRunnerContext().getBuild();
        // something logic with build instance

        BuildProgressLogger logger = agentRunningBuild.getBuildLogger();
        // something logic with logger instance (output information)

        LOG.info(agentRunningBuild.getSharedConfigParameters());

        File workingDirectory = getWorkingDirectory(); // get working directory
        Map<String, String> runnerParameters = getRunnerParameters(); // get runner parameters
        Map<String, String> sharedConfigParameters = agentRunningBuild.getSharedConfigParameters();

        BuildRunnerContext buildRunnerContext = getBuildRunnerContext();

        //Getting all the parameters

        scanConfig = new CheckmarxScanConfig();

        if (TRUE.equals(runnerParameters.get(USE_DEFAULT_SERVER))) {
            scanConfig.setClientId(validateNotEmpty(sharedConfigParameters.get(GLOBAL_AST_CLIENT_ID), GLOBAL_AST_CLIENT_ID));
            scanConfig.setServerUrl(validateNotEmpty(sharedConfigParameters.get(GLOBAL_AST_SERVER_URL), GLOBAL_AST_SERVER_URL));
            scanConfig.setAstSecret(PluginUtils.decrypt(validateNotEmpty(sharedConfigParameters.get(GLOBAL_AST_SECRET),GLOBAL_AST_SECRET)));
        }
        else {
            scanConfig.setServerUrl(validateNotEmpty(runnerParameters.get(SERVER_URL), SERVER_URL));
            scanConfig.setClientId(validateNotEmpty(runnerParameters.get(AST_CLIENT_ID), AST_CLIENT_ID));
            scanConfig.setAstSecret(PluginUtils.decrypt(validateNotEmpty(runnerParameters.get(AST_SECRET), AST_SECRET)));
        }


        ///////////------------
        LOG.info("-----------------------Checkmarx: Reached the BuildServiceAdapter------------------------");
        String checkmarxCliToolPath = getCheckmarxCliToolPath();

        String checkmarxAstSecret = getRunnerParameters().get(AST_SECRET);
//        if (nullIfEmpty(checkmarxAstSecret) == null) {
//            throw new RunBuildException("Checkmarx API key was not defined. Please configure the build properly and retry.");
//        }
        Map<String, String> envVars = new HashMap<>(getEnvironmentVariables());
        envVars.put("CX_APIKEY", checkmarxAstSecret);

        //  boolean result = submitDetailsToWrapper(LOG, checkmarxApiKey, checkmarxCliToolPath, (getWorkingDirectory().getAbsolutePath()));

        String sourceDir = getWorkingDirectory().getAbsolutePath();
        return new SimpleProgramCommandLine(envVars, getWorkingDirectory().getAbsolutePath(), checkmarxCliToolPath, getArguments());
    }

    @Override
    public void beforeProcessStarted() {
        getBuild().getBuildLogger().message("Scanning with Checkmarx...");
    }

    @Override
    List<String> getArguments() {
        List<String> arguments = new ArrayList<>();

        arguments.add("scan");
        arguments.add("create");

        //Set Origin
        arguments.add("--agent");
        arguments.add("TeamCity");

//        String serverUrl = getRunnerParameters().get(SERVER_URL);
//        if (nullIfEmpty(serverUrl) != null) {
//            arguments.add("--base-uri");
//            arguments.add(serverUrl);
//        }

        String projectName = getRunnerParameters().get(PROJECT_NAME);
        if (nullIfEmpty(projectName) != null) {
            arguments.add("--project-name");
            arguments.add(projectName);
        }

        arguments.add("--sources");
        arguments.add(".");

        arguments.add("--scan-types");
        arguments.add("sast");

//        String apiKey = getRunnerParameters().get(AST_SECRET);
//        if (nullIfEmpty(apiKey) != null) {
//            arguments.add("--apikey");
//            arguments.add(apiKey);
//        }

        String fileFilters = getRunnerParameters().get(ZIP_FILE_FILTERS);
        if (nullIfEmpty(fileFilters) != null) {
            arguments.add("--filter");
            arguments.add(fileFilters);
        }

        String additionalParameters = getRunnerParameters().get(ADDITIONAL_PARAMETERS);
        if (nullIfEmpty(additionalParameters) != null) {
            arguments.addAll(asList(additionalParameters.split("\\s+")));
        }


            arguments.add("--base-uri");
            arguments.add(scanConfig.getServerUrl()); //this property is probably null

        arguments.add("--client-id");
        arguments.add(scanConfig.getClientId()); //this property is probably null

        arguments.add("--secret");
        arguments.add(scanConfig.getAstSecret()); //this property is probably null

//
//        String severityThreshold = getRunnerParameters().get(ZIP_FILE_FILTERS);
//        arguments.add("--filters " + "\""+ severityThreshold + "\"");
//

//
//        String additionalParameters = getRunnerParameters().get(ADDITIONAL_PARAMETERS);
//        if (nullIfEmpty(additionalParameters) != null) {
//            arguments.addAll(asList(additionalParameters.split("\\s+")));
//        }

        return arguments;
    }

    private static String validateNotEmpty(String param, String paramName) throws InvalidParameterException {
        if (param == null || param.length() == 0) {
            throw new InvalidParameterException("Parameter [" + paramName + "] must not be empty");
        }
        return param;
    }

}

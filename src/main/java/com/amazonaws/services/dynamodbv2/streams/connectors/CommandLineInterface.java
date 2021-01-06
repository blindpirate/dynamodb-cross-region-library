/*
 * Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazonaws.services.dynamodbv2.streams.connectors;

import com.amazonaws.services.dynamodbv2.model.Record;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.kinesis.connectors.interfaces.IKinesisConnectorPipeline;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.annotations.VisibleForTesting;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.log4j.Log4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A command line interface allowing the connector to be launched independently from command line
 */
@Log4j
public class CommandLineInterface {

    /**
     * Command line main method entry point
     *
     * @param args
     *            command line arguments
     */
    public static void main(String[] args) {
        try {
            final Optional<Worker> workerOption = mainUnsafe(args);
            if (!workerOption.isPresent()) {
                return;
            }
            System.out.println("Starting replication now, check logs for more details.");
            workerOption.get().run();
        } catch (ParameterException e) {
            log.error(e);
            JCommander.getConsole().println(e.toString());
            System.exit(StatusCodes.EINVAL);
        } catch (Exception e) {
            log.fatal(e);
            JCommander.getConsole().println(e.toString());
            System.exit(StatusCodes.EINVAL);
        }
    }

    static Optional<Worker> mainUnsafe(String[] args) {
        // Initialize command line arguments and JCommander parser
        CommandLineArgs params = new CommandLineArgs();
        JCommander cmd = new JCommander(params);

        // parse given arguments
        cmd.parse(args);

        // show usage information if help flag exists
        if (params.isHelp()) {
            cmd.usage();
            return Optional.empty();
        }

        final CommandLineInterface cli = new CommandLineInterface(params);
        // create worker
        return Optional.of(cli.workerCreator.create());
    }

    @Getter(AccessLevel.PACKAGE)
    private final KinesisWorkerCreator workerCreator;

    public CommandLineInterface(KinesisWorkerCreator workerCreator) {
        this.workerCreator = workerCreator;
    }

    @VisibleForTesting
    CommandLineInterface(CommandLineArgs params) throws ParameterException {
        this(new KinesisWorkerCreator(params));
    }

    @SuppressWarnings("unchecked")
    private List<IKinesisConnectorPipeline<Record, Record>> initPipelines(String pipelineClassNames) {
        return Stream.of(pipelineClassNames.split(","))
                .map(fqcn -> {
                    try {
                        return (IKinesisConnectorPipeline<Record, Record>) Class.forName(fqcn).getConstructor().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
    }
}

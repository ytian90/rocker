/*
 * Copyright 2015 Fizzed Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fizzed.rocker.bin;

import ch.qos.logback.classic.Level;
import com.fizzed.rocker.compiler.RockerConfiguration;
import com.fizzed.rocker.compiler.RockerUtil;
import com.fizzed.rocker.compiler.TemplateParser;
import com.fizzed.rocker.model.*;
import com.fizzed.rocker.runtime.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class ParserMain {
    static private final Logger log = LoggerFactory.getLogger(ParserMain.class);
    
    static public void main(String[] args) {
        String rockerFile = System.getProperty("rocker.file");
        
        // set log level to trace @ runtime
        ch.qos.logback.classic.Logger rockerRootLogger
            = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("com.fizzed.rocker");
        rockerRootLogger.setLevel(Level.TRACE);
        
        RockerConfiguration configuration = new RockerConfiguration();
        configuration.setTemplateDirectory(new File("."));
        
        TemplateParser parser = new TemplateParser(configuration);
        
        File templateFile = new File(rockerFile);

        TemplateModel model = parse(parser, templateFile);
        
        logModel(model);
    }
    
    static public TemplateModel parse(TemplateParser parser, File f) {
        TemplateModel model = null;
        
        try {
            model = parser.parse(f);
        } catch (IOException | ParserException e) {
            if (e instanceof ParserException) {
                ParserException pe = (ParserException)e;
                log.error("{}:[{},{}] {}", f, pe.getLineNumber(), pe.getColumnNumber(), pe.getMessage());
            } else {
                log.error("Unable to cleanly parse template", e);
            }
            System.exit(1);
        }
        
        return model;
    }
    
    static public void logModel(TemplateModel model) {
        log.info("");
        log.info("--- template model ---");
        
        log.info("template: {}", model.getTemplateName());
        log.info("name: {}", model.getName());
        log.info("package: {}", model.getPackageName());
        log.info("content type: {}", model.getContentType());
        log.info("");
        
        for (JavaImport i : model.getImports()) {
            log.info("import: {}", i.getStatement());
            log.info(" src (@ {}): [{}]", i.getSourceRef(), i.getSourceRef().getConsoleFriendlyText());
        }
        
        for (Argument arg : model.getArguments()) {
            log.info("arg: {} {}", arg.getType(), arg.getName());
            log.info(" src (@ {}): [{}]", arg.getSourceRef(), arg.getSourceRef().getConsoleFriendlyText());
        }
        
        for (TemplateUnit unit : model.getUnits()) {
            if (unit instanceof PlainText) {
                PlainText plain = (PlainText)unit;
                log.info("plain: {}", RockerUtil.consoleFriendlyText(plain.getText()));
            } else if (unit instanceof Comment) {
                Comment comment = (Comment)unit;
                log.info("comment: {}", comment.getText());
            } else if (unit instanceof ValueExpression) {
                ValueExpression valueExpr = (ValueExpression)unit;
                log.info("value: {}", valueExpr.getExpression());
            } else if (unit instanceof NullTernaryExpression) {
                NullTernaryExpression nullTernaryExpr = (NullTernaryExpression)unit;
                log.info("nullTernary: {} ?: {}", nullTernaryExpr.getLeftExpression(), nullTernaryExpr.getRightExpression());
            } else if (unit instanceof BreakStatement) {
                log.info("break");
            } else if (unit instanceof ContinueStatement) {
                log.info("continue");
            } else if (unit instanceof ForBlockBegin) {
                ForBlockBegin block = (ForBlockBegin)unit;
                log.info("for begin: {}", block.getExpression());
            } else if (unit instanceof ForBlockEnd) {
                log.info("for end:");
            } else if (unit instanceof WithBlockBegin) {
                WithBlockBegin block = (WithBlockBegin)unit;
                log.info("with begin: isNullSafe={} ({} = {})", block.getStatement().isNullSafe(), block.getStatement().getVariables());
            } else if (unit instanceof WithBlockEnd) {
                log.info("with end:");
            } else if (unit instanceof IfBlockBegin) {
                IfBlockBegin block = (IfBlockBegin) unit;
                log.info("if begin: {}", block.getExpression());
            }
            else if (unit instanceof IfBlockElseIf) {
                IfBlockElseIf block = (IfBlockElseIf) unit;
                log.info("else if begin:", block.getExpression());
            } else if (unit instanceof IfBlockElse) {
                log.info("else begin:");
            } else if (unit instanceof IfBlockEnd) {
                log.info("if end:");
            } else {
                log.error("UH OH ARE YOU MISSING A MODEL TYPE???");
            }
            log.info(" src (@ {}): [{}]", unit.getSourceRef(), unit.getSourceRef().getConsoleFriendlyText());
        }
    }
}

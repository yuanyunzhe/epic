/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package chalk.tools.cmdline.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import nak.model.TrainUtil;
import chalk.tools.cmdline.AbstractTrainerTool;
import chalk.tools.cmdline.CmdLineUtil;
import chalk.tools.cmdline.TerminateToolException;
import chalk.tools.cmdline.params.EncodingParameter;
import chalk.tools.cmdline.params.TrainingToolParams;
import chalk.tools.cmdline.parser.ParserTrainerTool.TrainerToolParams;
import chalk.tools.dictionary.Dictionary;
import chalk.tools.parser.HeadRules;
import chalk.tools.parser.Parse;
import chalk.tools.parser.ParserModel;
import chalk.tools.parser.ParserType;
import chalk.tools.parser.chunking.Parser;
import chalk.tools.util.ObjectStream;
import chalk.tools.util.model.ModelUtil;


public final class ParserTrainerTool extends AbstractTrainerTool<Parse, TrainerToolParams> {
  
  interface TrainerToolParams extends TrainingParams, TrainingToolParams, EncodingParameter {
  }

  public ParserTrainerTool() {
    super(Parse.class, TrainerToolParams.class);
  }

  public String getShortDescription() {
    return "trains the learnable parser";
  }
  
  static Dictionary buildDictionary(ObjectStream<Parse> parseSamples, HeadRules headRules, int cutoff) {
    System.err.print("Building dictionary ...");
    
    Dictionary mdict;
    try {
      mdict = Parser.
          buildDictionary(parseSamples, headRules, cutoff);
    } catch (IOException e) {
      System.err.println("Error while building dictionary: " + e.getMessage());
      mdict = null;
    }
    System.err.println("done");
    
    return mdict;
  }
  
  static ParserType parseParserType(String typeAsString) {
    ParserType type = null;
    if(typeAsString != null && typeAsString.length() > 0) {
      type = ParserType.parse(typeAsString);
      if(type == null) {
        throw new TerminateToolException(1, "ParserType training parameter '" + typeAsString +
            "' is invalid!");
      }
    }
    
    return type;
  }
  
  // TODO: Add param to train tree insert parser
  public void run(String format, String[] args) {
    super.run(format, args);

    mlParams = CmdLineUtil.loadTrainingParameters(params.getParams(), true);
    
    if (mlParams != null) {
      if (!TrainUtil.isValid(mlParams.getSettings("build"))) {
        throw new TerminateToolException(1, "Build training parameters are invalid!");
      }
      
      if (!TrainUtil.isValid(mlParams.getSettings("check"))) {
        throw new TerminateToolException(1, "Check training parameters are invalid!");
      }
      
      if (!TrainUtil.isValid(mlParams.getSettings("attach"))) {
        throw new TerminateToolException(1, "Attach training parameters are invalid!");
      }
      
      if (!TrainUtil.isValid(mlParams.getSettings("tagger"))) {
        throw new TerminateToolException(1, "Tagger training parameters are invalid!");
      }
      
      if (!TrainUtil.isValid(mlParams.getSettings("chunker"))) {
        throw new TerminateToolException(1, "Chunker training parameters are invalid!");
      }
    }

    if(mlParams == null) {
      mlParams = ModelUtil.createTrainingParameters(params.getIterations(), params.getCutoff());
    }

    File modelOutFile = params.getModel();
    CmdLineUtil.checkOutputFile("parser model", modelOutFile);
    
    ParserModel model;
    try {

      // TODO hard-coded language reference
      HeadRules rules = new chalk.tools.parser.lang.en.HeadRules(
          new InputStreamReader(new FileInputStream(params.getHeadRules()),
              params.getEncoding()));
      
      ParserType type = parseParserType(params.getParserType());
      if(params.getFun()){
    	  Parse.useFunctionTags(true);
      }
      
      if (ParserType.CHUNKING.equals(type)) {
        model = chalk.tools.parser.chunking.Parser.train(
            factory.getLang(), sampleStream, rules,
            mlParams);
      }
      else if (ParserType.TREEINSERT.equals(type)) {
        model = chalk.tools.parser.treeinsert.Parser.train(factory.getLang(), sampleStream, rules,
            mlParams);
      }
      else {
        throw new IllegalStateException();
      }
    }
    catch (IOException e) {
      throw new TerminateToolException(-1, "IO error while reading training data or indexing data: "
          + e.getMessage(), e);
    }
    finally {
      try {
        sampleStream.close();
      } catch (IOException e) {
        // sorry that this can fail
      }
    }
    
    CmdLineUtil.writeModel("parser", modelOutFile, model);
  }
}

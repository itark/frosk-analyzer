/*
 Copyright (c) 2014 by Contributors

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package nu.itark.frosk.strategies.prediction.xgboost;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;

import java.util.HashMap;

/**
 * example for start from a initial base prediction
 *
 * @author hzx
 */
public class BoostFromPrediction {
  public static void main(String[] args) throws XGBoostError {
    System.out.println("start running example to start from a initial prediction");

    // load file from text file, also binary buffer generated by xgboost4j
    DMatrix trainMat = new DMatrix("/Users/fredrikmoller/temp/git/xgboost/demo/data/agaricus.txt.train?format=libsvm");
    DMatrix testMat = new DMatrix("/Users/fredrikmoller/temp/git/xgboost/demo/data/agaricus.txt.test?format=libsvm");

    //specify parameters
    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("eta", 1.0);
    params.put("max_depth", 2);
    params.put("silent", 1);
    params.put("objective", "binary:logistic");

    //specify watchList
    HashMap<String, DMatrix> watches = new HashMap<String, DMatrix>();
    watches.put("train", trainMat);
    watches.put("test", testMat);

    //train xgboost for 1 round
    Booster booster = XGBoost.train(trainMat, params, 1, watches, null, null);

    float[][] trainPred = booster.predict(trainMat, true);
    float[][] testPred = booster.predict(testMat, true);

    trainMat.setBaseMargin(trainPred);
    testMat.setBaseMargin(testPred);

    System.out.println("result of running from initial prediction");
    Booster booster2 = XGBoost.train(trainMat, params, 1, watches, null, null);

    System.out.println("booster2:"+booster2);

  }
}

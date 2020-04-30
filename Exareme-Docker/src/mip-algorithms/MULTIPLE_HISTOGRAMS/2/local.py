from __future__ import division
from __future__ import print_function

import sys
from os import path
from argparse import ArgumentParser
import numpy as np
import logging

sys.path.append(path.dirname(path.dirname(path.dirname(path.abspath(__file__)))) + '/utils/')
sys.path.append(path.dirname(path.dirname(path.dirname(path.abspath(__file__)))) + '/MULTIPLE_HISTOGRAMS/')

from algorithm_utils import StateData,Global2Local_TD, PRIVACY_MAGIC_NUMBER, init_logger
from multhist_lib import multipleHist2_Loc2Glob_TD


def run_local_step(args_X, args_Y, args_bins, dataSchema, CategoricalVariablesWithDistinctValues, dataFrame, globalStatistics):
    # Local Part of the algorithm
    Hist = dict()
    for varx in args_X:
        if varx in CategoricalVariablesWithDistinctValues: # varx is  categorical
            # Histogram categorical of varx
            df_count = dataFrame.groupby(varx)[varx].count()
            categories = CategoricalVariablesWithDistinctValues[varx]
            data = []
            for groupLevelx in CategoricalVariablesWithDistinctValues[varx]:
                if groupLevelx in dataFrame[varx].unique():
                    data.append(df_count[groupLevelx])
                else:
                    data.append(0)
            Hist[varx,None] = {"Data" :  data, "Categoriesx" : categories, "Categoriesy" :None}

            for vary in args_Y:
                #Histogram thn varx group by var y
                df_count = dataFrame.groupby([varx,vary])[varx].count()
                dataTotal = []
                for groupLevely in CategoricalVariablesWithDistinctValues[vary]:
                    data = []
                    for groupLevelx in CategoricalVariablesWithDistinctValues[varx]:
                        if (groupLevelx, groupLevely) in zip(dataFrame[varx],dataFrame[vary]) :
                            data.append(df_count[groupLevelx][groupLevely])
                        else:
                            data.append (0)
                    dataTotal.append(data)
                Hist[varx,vary] = {"Data" :  dataTotal, "Categoriesx" : categories, "Categoriesy" :CategoricalVariablesWithDistinctValues[vary]  }

        if varx not in  CategoricalVariablesWithDistinctValues: # varx is not categorical
             logging.debug(["A. globalStatistics[varx,None,None,None]['count']=", globalStatistics[varx,None,None,None]['count']])
             if globalStatistics[varx,None,None,None]['count'] > PRIVACY_MAGIC_NUMBER :
                 dataFrameNew = dataFrame[varx].dropna()
                 logging.debug(["A. globalStatistics[varx,None,None,None]['min']=", globalStatistics[varx,None,None,None]['min']])
                 logging.debug(["A. globalStatistics[varx,None,None,None]['max']=",globalStatistics[varx,None,None,None]['max']])
                 logging.debug(["A. args_bins[varx]=", args_bins[varx]])
                 myhist = [x.tolist() for x in np.histogram(dataFrameNew, range = [ globalStatistics[varx,None,None,None]['min'],
                                                                                                globalStatistics[varx,None,None,None]['max']],  bins = args_bins[varx])]
                 mycategories = [str(myhist[1][i+1])+"-"+str(myhist[1][i]) for i in range(len(myhist[1])-1)]
                 Hist[varx,None] = { "Data" : myhist[0], "Categoriesx" : mycategories , "Categoriesy" : None }
             else:
                 mycategories = None
                 Hist[varx,None] = { "Data" : [0]*args_bins[varx], "Categoriesx" : mycategories , "Categoriesy" : None }
             #Histogram thn varx group by var y
             for vary in args_Y:
                 dfs = dataFrame.groupby(vary)[varx]
                 data = []

                 for groupLevely in CategoricalVariablesWithDistinctValues[vary]:
                    if groupLevely in dfs.groups:
                        df = dfs.get_group(groupLevely)
                        logging.debug(["B.  globalStatistics[varx,vary,None,groupLevely]['count'] =", globalStatistics[varx,vary,None,groupLevely]['count'] ])
                        if  globalStatistics[varx,vary,None,groupLevely]['count'] > PRIVACY_MAGIC_NUMBER :
                            dfNew = df.dropna()
                            logging.debug(["B.globalStatistics[varx,vary,None,groupLevely]['min']=",globalStatistics[varx,vary,None,groupLevely]['min']])
                            logging.debug(["B.globalStatistics[varx,vary,None,groupLevely]['max']=",globalStatistics[varx,vary,None,groupLevely]['max']])
                            logging.debug(["B. args_bins[varx]=", args_bins[varx]])
                            myhist =  [x.tolist() for x in np.histogram(dfNew, range = [globalStatistics[varx,vary,None,groupLevely]['min'],
                                                                         globalStatistics[varx,vary,None,groupLevely]['max']], bins = args_bins[varx])]
                            data.append(myhist[0])
                        else:
                            data.append([0]*args_bins[varx])
                    else:
                        data.append([0]*args_bins[varx])
                    Hist[varx,vary] = {  "Data" : data ,  "Categoriesx" : mycategories, "Categoriesy" : CategoricalVariablesWithDistinctValues[vary] }

    # Histogram modification due to privacy
    # for key in Hist:
    #     for i in xrange(len(Hist[key]['Data'])):
    #         if isinstance(Hist[key]['Data'][i], list):
    #             for j in xrange(len(Hist[key]['Data'][i])):
    #                 if Hist[key]['Data'][i][j] <= PRIVACY_MAGIC_NUMBER:
    #                  Hist[key]['Data'][i][j] = 0
    #         else:
    #             if Hist[key]['Data'][i] <= PRIVACY_MAGIC_NUMBER:
    #                 Hist[key]['Data'][i] = 0

    return Hist


def main():
    # Parse arguments
    parser = ArgumentParser()
    parser.add_argument('-prev_state_pkl', required=True, help='Path to the pickle file holding the previous state.')
    parser.add_argument('-global_step_db', required=True, help='Path to db holding global step results.')
    args, unknown = parser.parse_known_args()
    fname_prev_state = path.abspath(args.prev_state_pkl)
    global_db = path.abspath(args.global_step_db)

    # Load local state
    local_state = StateData.load(fname_prev_state).get_data()
    # Load global node output
    globalStatistics = Global2Local_TD.load(global_db).get_data()['global_in']

    init_logger()
    logging.debug("LOCAL2")
    logging.debug(["args_X, args_Y, args_bins, dataSchema, CategoricalVariablesWithDistinctValues:", args_X, args_Y, args_bins, dataSchema, CategoricalVariablesWithDistinctValues])
    logging.debug(["dataFrame=",local_state['dataFrame']])
    logging.debug(["globalStatistics=", globalStatistics])

    # Run algorithm local step
    Hist = run_local_step(local_state['args_X'], local_state['args_Y'] ,
                               local_state['args_bins'], local_state['dataSchema'],
                               local_state['CategoricalVariablesWithDistinctValues'], local_state['dataFrame'],
                               globalStatistics)

    # Pack results
    local_out = multipleHist2_Loc2Glob_TD(local_state['args_X'], local_state['args_Y'] , local_state['CategoricalVariablesWithDistinctValues'], Hist)
    # Return the output data
    local_out.transfer()


if __name__ == '__main__':
    main()

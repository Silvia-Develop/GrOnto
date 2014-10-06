# -*- coding: cp1252 -*-

import sys
import os
import math
import traceback

def loadSample(queryfile, grontofile, sortedquery, cutAt=0):

    # gronto data structures    
    grontoResCat = {}
    grontoCatRes = {}
    
    f = open(grontofile, 'r')
    try:
        for line in f:
            line = line.replace("\n","")
            if len(line)==0 or line[0]=='#':
                continue
            
            resId, category = line.split(':')
            category = category.lower()

            if cutAt!=0 and int(resId)>cutAt:
                continue
            
            if resId not in grontoResCat:
                grontoResCat[resId] = []
            grontoResCat[resId].append(category)            
            
            if category not in grontoCatRes:
                grontoCatRes[category] = []
            grontoCatRes[category].append(resId)
            
        for category in grontoCatRes:
            l = [ int(i) for i in grontoCatRes[category]]
            l.sort()
            grontoCatRes[category] = [str(i) for i in l]
                
    except Exception, e:
        print "Unable to open gronto file: %s"%grontofile, e
        traceback.print_exc()
        sys.exit(1)
    finally:
        f.close()
    
    # categorization provided by the user
    queryResCat = {}
    f = open(queryfile, 'r')
    try:
        for line in f:
            line = line.replace("\n","")
            if len(line)==0 or line[0]=='#':
                continue
            
            resId, category = line.split(':')
            category = category.lower()

            if cutAt>0 and int(resId)>cutAt:
                continue
            
            if resId not in queryResCat:
                queryResCat[resId] = []    
            queryResCat[resId].append( category.strip() )
            
    except Exception, e:
        print "Unable to open query file: %s"%(queryfile), e
        traceback.print_exc()
        sys.exit(1)
    finally:
        f.close()

    # user judgement sorting
    # formato:: categoria:23,45,99,134
    # nome file:: query.._sorting.txt
    queryCatRes = {}
    f = open(sortedquery, 'r')
    try:
        for line in f:
            line = line.replace("\n","")
            if len(line)==0 or line[0]=='#':
                continue
            
            category, resList = line.split(':')
            category = category.lower()

            resList = [r.strip() for r in resList.split(',')]
            if cutAt>0:
                resList = [r for r in resList if int(r) <= cutAt]
            queryCatRes[category] = resList
                
    except Exception, e:
        print "Unable to open sort file %s"%(sortedquery), e
        traceback.print_exc()
        sys.exit(1)
    finally:
        f.close()
        
    return (queryResCat, queryCatRes, grontoResCat, grontoCatRes)

def coverageMeasure(queryResCat, queryCatRes, grontoResCat, grontoCatRes):
    cvr = 0.0

    for resId in queryResCat:
        rcSet = set(queryResCat[resId])
        rcSize = float(len(rcSet))

        if resId in grontoResCat:
            grcSet = set(grontoResCat[resId])
        else:
            grcSet = set([])

        rcSet = rcSet.intersection( grcSet )
        cvr += len(rcSet)/rcSize
        
    cvr /= len(queryResCat)
    return cvr

def ndcgMeasure(queryResCat, queryCatRes, grontoResCat, grontoCatRes):
    ndcg = 0.0
    for cat in queryCatRes:
        dcgQuery = 0.0
        for resId in queryCatRes[cat]:
            dcgQuery += 1.0/math.log(1.0+float(resId))

        if (dcgQuery == 0.0):
            # no contribution from this category
            continue
        else:
            # evaluate the ndcg contribution
            dcgGronto = 0.0
            if cat in grontoCatRes:
                for resId in grontoCatRes[cat]:
                    dcgGronto += 1.0/math.log(1.0+float(resId))
                
            ndcg += dcgGronto / dcgQuery
            
    ndcg /= len(grontoCatRes)
    return ndcg

def precisionMeasure(queryResCat, queryCatRes, grontoResCat, grontoCatRes):
    prec = 0.0
    totp = 0
    for cat in grontoCatRes:
        if cat not in queryCatRes:
            continue
        precisionCat = 1.0 * len( set(queryCatRes[cat]).intersection(set(grontoCatRes[cat])) )
        precisionCat /= len(set(grontoCatRes[cat]))
        totp += 1
        prec += precisionCat
    return prec/totp

def recallMeasure(queryResCat, queryCatRes, grontoResCat, grontoCatRes):
    rec = 0.0
    totr = 0
    for cat in grontoCatRes:
        if cat not in queryCatRes:
            continue  
        recallCat = 1.0 * len(set(queryCatRes[cat]).intersection(set(grontoCatRes[cat])))
        recallCat /= len(set(queryCatRes[cat]))
        totr += 1
        rec  += recallCat
    return rec/totr

def main(argv):
    sampleDir = argv[0]
    
    # span the working directory and get the relevant files
    measures = {}
    tagId = 0
    for root, dirs, files in os.walk(sampleDir):
        sampleTag = [ f.replace("_query.txt", "") for f in files if f.startswith("query=") and f.endswith("_query.txt")]
        for t in sampleTag:
            tagId += 1

            g = os.path.join(root, t + "_gronto.txt")
            q = os.path.join(root, t + "_query.txt")
            s = os.path.join(root, t + "_sorting.txt")
            
            # load data          
            qRes, qCat, gRes, gCat = loadSample(q, g, s, cutAt=25)

            # evaluate the coverage metric
            m1 = coverageMeasure(qRes, qCat, gRes, gCat)
        
            # evaluate the NDCG
            m2 = ndcgMeasure(qRes, qCat, gRes, gCat)

            m3 = precisionMeasure(qRes, qCat, gRes, gCat)
            m4 = recallMeasure(qRes, qCat, gRes, gCat)

            # put here other measures ... //TODO
            measures[tagId] = [m1, m2, m3, m4]

    # momentum for the data
    avgCov = 0.0
    avgNdcg = 0.0
    avgPrec = 0.0
    avgRec = 0.0
    
    for t in measures:
        avgCov += measures[t][0]
        avgNdcg += measures[t][1]
        avgPrec += measures[t][2]
        avgRec += measures[t][3]
    
    avgCov /= len(measures)
    avgNdcg /= len(measures)
    avgPrec /= len(measures)
    avgRec /= len(measures)    

    stdevCov = 0.0
    stdevNdcg = 0.0
    stdevPrec = 0.0
    stdevRec = 0.0
    
    for t in measures:
        stdevCov += (measures[t][0]-avgCov)*(measures[t][0]-avgCov)
        stdevNdcg += (measures[t][1]-avgNdcg)*(measures[t][1]-avgNdcg)
        stdevPrec += (measures[t][2]-avgPrec)**2
        stdevRec += (measures[t][3]-avgRec)**2

    stdevCov = math.sqrt(stdevCov / len(measures))
    stdevNdcg = math.sqrt(stdevNdcg / len(measures))
    stdevPrec = math.sqrt(stdevPrec / len(measures))
    stdevRec = math.sqrt(stdevRec / len(measures))
    
    print "#average: coverage=%f, ndcg=%f, prec=%f, rec=%f"%(avgCov, avgNdcg, avgPrec, avgRec)
    print "#stddev: coverage=%f, ndcg=%f, prec=%f, rec=%f"%(stdevCov, stdevNdcg, stdevPrec, stdevRec)

    #  printout
    #print "sample","coverage","NDCG","Prec","Rec"
    #for t in measures:
    #    print "%d %.4f %.4f %.4f %.4f"%(t, measures[t][0], measures[t][1], measures[t][2], measures[t][3])
    
    return 0

if __name__ == '__main__':
    #fakeArgv = ['H:\\SviluppoSoftware\\MyTest'] # Per test
    #main(fakeArgv)
    sys.exit( main(sys.argv[1:]) )
    

package metaMorphic;

import org.chocosolver.solver.constraints.Constraint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TriMCT {

    public List<int[]> sourceSet;

    public MetaRelation metaRelation;

    public List<int[]> followSet;

    public TriMCT(MetaRelation metaRelation, List<int[]> sourceSet){
        this.metaRelation = metaRelation;
        this.sourceSet = sourceSet;
        followSet = new ArrayList<int[]>();
    }

    public List<List<int[]>> process(ArrayList<ArrayList<String>> info){
        List<List<int[]>> ans = new ArrayList<>();
        for(int[] source : sourceSet){
            boolean flag = false;
            List<int[]> temp = new ArrayList<>();
            ArrayList<String> tempInfo = new ArrayList<>();
            int  i= 0 ;
            for(Constraint[] cs : this.metaRelation.MR){
                int[] follow = this.metaRelation.genOne(source, cs);
                if(follow != null) {
                    if(!flag){
                        flag = true;
                        temp.add(source);
                        tempInfo.add("source");
                    }
                    temp.add(follow);
                    tempInfo.add(metaRelation.MRInfo.get(i));
                    this.followSet.add(follow);
                }
                i++;
            }
            if(!temp.isEmpty()){
                info.add(tempInfo);
                ans.add(temp);
            }
        }
        return ans;
    }
}

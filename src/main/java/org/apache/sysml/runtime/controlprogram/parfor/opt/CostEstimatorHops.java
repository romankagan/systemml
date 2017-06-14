/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sysml.runtime.controlprogram.parfor.opt;

import org.apache.sysml.api.RuntimePlatform;
import org.apache.sysml.hops.Hop;
import org.apache.sysml.hops.OptimizerUtils;
import org.apache.sysml.lops.LopProperties.ExecType;
import org.apache.sysml.runtime.DMLRuntimeException;
import org.apache.sysml.runtime.controlprogram.parfor.opt.OptNode.NodeType;
import org.apache.sysml.runtime.controlprogram.parfor.opt.Optimizer.CostModelType;
import org.apache.sysml.runtime.controlprogram.parfor.stat.InfrastructureAnalyzer;

public class CostEstimatorHops extends CostEstimator
{
	public static double DEFAULT_MEM_MR = -1;
	public static double DEFAULT_MEM_SP = 20*1024*1024;
	
	private OptTreePlanMappingAbstract _map = null;
	
	static {
		DEFAULT_MEM_MR = DEFAULT_MEM_ESTIMATE_MR; //20MB
		if( InfrastructureAnalyzer.isLocalMode() )
			DEFAULT_MEM_MR = DEFAULT_MEM_MR + InfrastructureAnalyzer.getRemoteMaxMemorySortBuffer();
	}
	
	
	public CostEstimatorHops( OptTreePlanMappingAbstract map ) {
		_map = map;
	}

	@Override
	public double getLeafNodeEstimate(TestMeasure measure, OptNode node)
		throws DMLRuntimeException 
	{
		if( node.getNodeType() != NodeType.HOP )
			return 0; //generic optnode but no childs (e.g., PB for rmvar inst)
		
		if( measure != TestMeasure.MEMORY_USAGE )
			throw new DMLRuntimeException( "Testmeasure "+measure+" not supported by cost model "+CostModelType.STATIC_MEM_METRIC+"." );
		
		//core mem estimation (use hops estimate)
		Hop h = _map.getMappedHop( node.getID() );
		double value = h.getMemEstimate();
		
		//handle specific cases 
		double DEFAULT_MEM_REMOTE = OptimizerUtils.isSparkExecutionMode() ? 
								DEFAULT_MEM_SP : DEFAULT_MEM_MR;
		
		if( value >= DEFAULT_MEM_REMOTE )   	  
		{
			//check for CP estimate but MR type
			if( h.getExecType()==ExecType.MR ) 
			{
				value = DEFAULT_MEM_REMOTE;
			}
			//check for CP estimate but Spark type (include broadcast requirements)
			else if( h.getExecType()==ExecType.SPARK )
			{
				value = DEFAULT_MEM_REMOTE + h.getSpBroadcastSize();
			}
			//check for invalid cp memory estimate
			else if ( h.getExecType()==ExecType.CP && value >= OptimizerUtils.getLocalMemBudget() )
			{
				if( RuntimePlatform.rtplatform != RuntimePlatform.ExecutionMode.SINGLE_NODE && h.getForcedExecType()==null )
					LOG.warn("Memory estimate larger than budget but CP exec type (op="+h.getOpString()+", name="+h.getName()+", memest="+h.getMemEstimate()+").");
				value = DEFAULT_MEM_REMOTE;
			}
			//check for non-existing exec type
			else if ( h.getExecType()==null)
			{
				//note: if exec type is 'null' lops have never been created (e.g., r(T) for tsmm),
				//in that case, we do not need to raise a warning 
				value = DEFAULT_MEM_REMOTE;
			}
		}
		
		//check for forced runtime platform
		if( h.getForcedExecType()==ExecType.MR  || h.getForcedExecType()==ExecType.SPARK) 
		{
			value = DEFAULT_MEM_REMOTE;
		}
		
		if( value <= 0 ) //no mem estimate
		{
			LOG.warn("Cannot get memory estimate for hop (op="+h.getOpString()+", name="+h.getName()+", memest="+h.getMemEstimate()+").");
			value = CostEstimator.DEFAULT_MEM_ESTIMATE_CP;
		}
		
		LOG.trace("Memory estimate "+h.getName()+", "+h.getOpString()+"("+node.getExecType()+")"+"="+OptimizerRuleBased.toMB(value));
		
		return value;
	}

	@Override
	public double getLeafNodeEstimate(TestMeasure measure, OptNode node, ExecType et)
		throws DMLRuntimeException 
	{
		if( node.getNodeType() != NodeType.HOP )
			return 0; //generic optnode but no childs (e.g., PB for rmvar inst)
		
		if( measure != TestMeasure.MEMORY_USAGE )
			throw new DMLRuntimeException( "Testmeasure "+measure+" not supported by cost model "+CostModelType.STATIC_MEM_METRIC+"." );
		
		//core mem estimation (use hops estimate)
		Hop h = _map.getMappedHop( node.getID() );
		double value = h.getMemEstimate();
		if( et != ExecType.CP ) //MR, null
			value = DEFAULT_MEM_MR;
		if( value <= 0 ) //no mem estimate
			value = CostEstimator.DEFAULT_MEM_ESTIMATE_CP;
		
		LOG.trace("Memory estimate (forced exec type) "+h.getName()+", "+h.getOpString()+"("+node.getExecType()+")"+"="+OptimizerRuleBased.toMB(value));
		
		return value;
	}
}

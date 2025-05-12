import React, { useState } from 'react';
import { GitBranch, AlertCircle, Zap } from 'lucide-react';

/**
 * 语义分析面板 - 提供多种代码语义分析功能
 */
function SemanticAnalysisPanel({ projectId }) {
    const [analysisType, setAnalysisType] = useState('callGraph');
    const [methodId, setMethodId] = useState('');
    const [direction, setDirection] = useState('callees');
    const [concept, setConcept] = useState('');
    const [analysisResults, setAnalysisResults] = useState(null);
    const [isAnalyzing, setIsAnalyzing] = useState(false);
    const [analysisError, setAnalysisError] = useState('');

    const handleAnalysis = async (e) => {
        e.preventDefault();

        try {
            setIsAnalyzing(true);
            setAnalysisError('');

            let url;
            if (analysisType === 'callGraph') {
                if (!methodId) {
                    setAnalysisError('请输入方法ID');
                    setIsAnalyzing(false);
                    return;
                }
                url = `/api/v1/projects/${projectId}/semantic/calls?methodId=${encodeURIComponent(methodId)}&direction=${direction}`;
            } else if (analysisType === 'dataFlow') {
                if (!methodId) {
                    setAnalysisError('请输入方法ID');
                    setIsAnalyzing(false);
                    return;
                }
                url = `/api/v1/projects/${projectId}/semantic/dataflow?methodId=${encodeURIComponent(methodId)}`;
            } else if (analysisType === 'similar') {
                if (!methodId) {
                    setAnalysisError('请输入方法ID');
                    setIsAnalyzing(false);
                    return;
                }
                url = `/api/v1/projects/${projectId}/semantic/similar?methodId=${encodeURIComponent(methodId)}&minSimilarity=0.7`;
            } else if (analysisType === 'concept') {
                if (!concept) {
                    setAnalysisError('请输入概念关键词');
                    setIsAnalyzing(false);
                    return;
                }
                url = `/api/v1/projects/${projectId}/semantic/concepts?concept=${encodeURIComponent(concept)}`;
            }

            const response = await fetch(url);

            if (response.ok) {
                const data = await response.json();
                setAnalysisResults(data);
                if ((Array.isArray(data) && data.length === 0) || (!Array.isArray(data) && Object.keys(data).length === 0)) {
                    setAnalysisError('没有找到相关数据');
                }
            } else {
                const errorData = await response.json();
                setAnalysisError(errorData.message || '分析失败');
            }
        } catch (error) {
            setAnalysisError('分析过程中发生错误');
        } finally {
            setIsAnalyzing(false);
        }
    };

    const renderAnalysisResults = () => {
        if (!analysisResults) return null;

        switch (analysisType) {
            case 'callGraph':
                return (
                    <div className="space-y-4">
                        <h3 className="text-lg font-medium text-gray-800">调用关系 ({direction === 'callees' ? '被调用方法' : '调用者'})</h3>
                        {Array.isArray(analysisResults) && analysisResults.length > 0 ? (
                            <ul className="divide-y divide-gray-200">
                                {analysisResults.map((method, index) => (
                                    <li key={index} className="py-2">
                                        <div className="flex items-center">
                                            <GitBranch className="h-4 w-4 text-blue-500 mr-2" />
                                            <span className="text-gray-800">{method}</span>
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        ) : (
                            <p className="text-gray-600">没有找到相关调用关系</p>
                        )}
                    </div>
                );

            case 'dataFlow':
                return (
                    <div className="space-y-4">
                        <h3 className="text-lg font-medium text-gray-800">数据流分析</h3>
                        {analysisResults && (
                            <div className="border border-gray-200 rounded-md p-4">
                                <h4 className="font-medium text-gray-800 mb-2">方法: {analysisResults.methodId}</h4>

                                <div className="mb-4">
                                    <h5 className="text-sm font-medium text-gray-700 mb-1">输入参数:</h5>
                                    {Object.keys(analysisResults.inputs).length > 0 ? (
                                        <ul className="space-y-1">
                                            {Object.entries(analysisResults.inputs).map(([name, type], index) => (
                                                <li key={index} className="text-sm text-gray-600">
                                                    <span className="font-mono bg-gray-100 px-1 rounded">{name}</span>: {type}
                                                </li>
                                            ))}
                                        </ul>
                                    ) : (
                                        <p className="text-sm text-gray-600">无输入参数</p>
                                    )}
                                </div>

                                <div className="mb-4">
                                    <h5 className="text-sm font-medium text-gray-700 mb-1">输出:</h5>
                                    {Object.keys(analysisResults.outputs).length > 0 ? (
                                        <ul className="space-y-1">
                                            {Object.entries(analysisResults.outputs).map(([name, type], index) => (
                                                <li key={index} className="text-sm text-gray-600">
                                                    <span className="font-mono bg-gray-100 px-1 rounded">{name}</span>: {type}
                                                </li>
                                            ))}
                                        </ul>
                                    ) : (
                                        <p className="text-sm text-gray-600">无输出</p>
                                    )}
                                </div>

                                <div>
                                    <h5 className="text-sm font-medium text-gray-700 mb-1">连接到:</h5>
                                    {analysisResults.connections.length > 0 ? (
                                        <ul className="space-y-1">
                                            {analysisResults.connections.map((connection, index) => (
                                                <li key={index} className="text-sm text-gray-600">
                                                    {connection}
                                                </li>
                                            ))}
                                        </ul>
                                    ) : (
                                        <p className="text-sm text-gray-600">无连接</p>
                                    )}
                                </div>
                            </div>
                        )}
                    </div>
                );

            case 'similar':
                return (
                    <div className="space-y-4">
                        <h3 className="text-lg font-medium text-gray-800">相似方法</h3>
                        {Array.isArray(analysisResults) && analysisResults.length > 0 ? (
                            <ul className="divide-y divide-gray-200">
                                {analysisResults.map((pair, index) => (
                                    <li key={index} className="py-3">
                                        <div className="flex items-center justify-between">
                                            <div>
                                                <p className="text-gray-800 font-medium">
                                                    {pair.method1Id === methodId ? pair.method2Id : pair.method1Id}
                                                </p>
                                                <p className="text-sm text-gray-600">与 {methodId} 的相似度</p>
                                            </div>
                                            <span className="px-2 py-1 text-xs font-medium rounded-full bg-blue-100 text-blue-800">
                        {Math.round(pair.similarity * 100)}%
                      </span>
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        ) : (
                            <p className="text-gray-600">没有找到相似方法</p>
                        )}
                    </div>
                );

            case 'concept':
                return (
                    <div className="space-y-4">
                        <h3 className="text-lg font-medium text-gray-800">概念相关实体</h3>
                        {Array.isArray(analysisResults) && analysisResults.length > 0 ? (
                            <ul className="divide-y divide-gray-200">
                                {analysisResults.map((result, index) => (
                                    <li key={index} className="py-3">
                                        <div className="flex items-start">
                                            <div className="flex-1">
                                                <p className="text-gray-800 font-medium">{result.entityId}</p>
                                                <p className="text-sm text-gray-600">概念: {result.concept}</p>
                                            </div>
                                            <span className="px-2 py-1 text-xs font-medium rounded-full bg-blue-100 text-blue-800">
                        {result.source}
                      </span>
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        ) : (
                            <p className="text-gray-600">没有找到相关实体</p>
                        )}
                    </div>
                );

            default:
                return null;
        }
    };

    return (
        <div>
            <h2 className="text-xl font-semibold text-gray-800 mb-6">语义分析</h2>

            <div className="bg-white shadow-sm rounded-lg p-6 mb-6">
                <form onSubmit={handleAnalysis} className="space-y-4">
                    <div className="flex space-x-4">
                        <div>
                            <label htmlFor="analysis-type" className="block text-sm font-medium text-gray-700 mb-1">分析类型</label>
                            <select
                                id="analysis-type"
                                value={analysisType}
                                onChange={(e) => setAnalysisType(e.target.value)}
                                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-blue-500"
                            >
                                <option value="callGraph">调用图分析</option>
                                <option value="dataFlow">数据流分析</option>
                                <option value="similar">相似方法分析</option>
                                <option value="concept">概念分析</option>
                            </select>
                        </div>

                        {(analysisType === 'callGraph' || analysisType === 'dataFlow' || analysisType === 'similar') && (
                            <div className="flex-1">
                                <label htmlFor="method-id" className="block text-sm font-medium text-gray-700 mb-1">方法ID</label>
                                <input
                                    id="method-id"
                                    type="text"
                                    value={methodId}
                                    onChange={(e) => setMethodId(e.target.value)}
                                    placeholder="例如：ClassName#methodName"
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-blue-500"
                                />
                            </div>
                        )}

                        {analysisType === 'callGraph' && (
                            <div>
                                <label htmlFor="direction" className="block text-sm font-medium text-gray-700 mb-1">方向</label>
                                <select
                                    id="direction"
                                    value={direction}
                                    onChange={(e) => setDirection(e.target.value)}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-blue-500"
                                >
                                    <option value="callees">被调用方法</option>
                                    <option value="callers">调用者</option>
                                </select>
                            </div>
                        )}

                        {analysisType === 'concept' && (
                            <div className="flex-1">
                                <label htmlFor="concept" className="block text-sm font-medium text-gray-700 mb-1">概念关键词</label>
                                <input
                                    id="concept"
                                    type="text"
                                    value={concept}
                                    onChange={(e) => setConcept(e.target.value)}
                                    placeholder="例如：数据库、连接池、线程安全"
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-blue-500"
                                />
                            </div>
                        )}
                    </div>

                    {analysisError && (
                        <div className="bg-red-50 border-l-4 border-red-500 p-4">
                            <div className="flex">
                                <AlertCircle className="h-5 w-5 text-red-500 mr-2" />
                                <p className="text-red-700">{analysisError}</p>
                            </div>
                        </div>
                    )}

                    <div className="flex justify-end">
                        <button
                            type="submit"
                            disabled={isAnalyzing}
                            className="flex items-center px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md"
                        >
                            {isAnalyzing ? (
                                <>
                                    <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                    分析中...
                                </>
                            ) : (
                                <>
                                    <Zap className="h-4 w-4 mr-2" />
                                    分析
                                </>
                            )}
                        </button>
                    </div>
                </form>
            </div>

            {/* 分析结果 */}
            {analysisResults && (
                <div className="bg-white shadow-sm rounded-lg p-6">
                    {renderAnalysisResults()}
                </div>
            )}
        </div>
    );
}

export default SemanticAnalysisPanel;
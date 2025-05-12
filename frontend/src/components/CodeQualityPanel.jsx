import React, { useState, useEffect } from 'react';
import { AlertCircle } from 'lucide-react';

/**
 * 代码质量面板 - 显示代码质量问题和评分
 */
function CodeQualityPanel({ projectId }) {
    const [qualityIssues, setQualityIssues] = useState([]);
    const [entityId, setEntityId] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const [qualityScore, setQualityScore] = useState(null);

    useEffect(() => {
        const fetchQualityIssues = async () => {
            try {
                setIsLoading(true);
                setError('');

                const url = `/api/v1/projects/${projectId}/semantic/quality${entityId ? `?entityId=${encodeURIComponent(entityId)}` : ''}`;
                const response = await fetch(url);

                if (response.ok) {
                    const data = await response.json();
                    setQualityIssues(data);

                    // 简单计算质量得分
                    const totalIssues = data.length;
                    const errorCount = data.filter(issue => issue.severity === 'ERROR').length;
                    const warningCount = data.filter(issue => issue.severity === 'WARNING').length;
                    const infoCount = data.filter(issue => issue.severity === 'INFO').length;

                    // 基础分100，错误-10，警告-2，信息-0.5
                    const score = Math.max(0, Math.min(100, 100 - (errorCount * 10 + warningCount * 2 + infoCount * 0.5)));
                    setQualityScore(Math.round(score));
                } else {
                    const errorData = await response.json();
                    setError(errorData.message || '获取代码质量数据失败');
                }
            } catch (error) {
                setError('获取代码质量数据时发生错误');
            } finally {
                setIsLoading(false);
            }
        };

        fetchQualityIssues();
    }, [projectId, entityId]);

    const getSeverityColor = (severity) => {
        switch (severity) {
            case 'ERROR':
                return 'bg-red-100 text-red-800';
            case 'WARNING':
                return 'bg-yellow-100 text-yellow-800';
            case 'INFO':
                return 'bg-blue-100 text-blue-800';
            default:
                return 'bg-gray-100 text-gray-800';
        }
    };

    const getIssueTypeIcon = (type) => {
        switch (type) {
            case 'LONG_METHOD':
            case 'LARGE_CLASS':
                return <svg className="h-4 w-4 text-gray-500" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>;
            case 'TOO_MANY_PARAMETERS':
                return <svg className="h-4 w-4 text-gray-500" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><ellipse cx="12" cy="5" rx="9" ry="3"></ellipse><path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"></path><path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"></path></svg>;
            case 'MISSING_JAVADOC':
                return <svg className="h-4 w-4 text-gray-500" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>;
            case 'INCONSISTENT_NAMING':
                return <svg className="h-4 w-4 text-gray-500" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="16 18 22 12 16 6"></polyline><polyline points="8 6 2 12 8 18"></polyline></svg>;
            default:
                return <svg className="h-4 w-4 text-gray-500" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>;
        }
    };

    return (
        <div>
            <h2 className="text-xl font-semibold text-gray-800 mb-6">代码质量分析</h2>

            <div className="bg-white shadow-sm rounded-lg p-6 mb-6">
                <div className="flex space-x-4 items-end">
                    <div className="flex-1">
                        <label htmlFor="entity-id" className="block text-sm font-medium text-gray-700 mb-1">实体ID (可选)</label>
                        <input
                            id="entity-id"
                            type="text"
                            value={entityId}
                            onChange={(e) => setEntityId(e.target.value)}
                            placeholder="留空查看所有问题，或输入特定实体ID"
                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-blue-500"
                        />
                    </div>

                    <button
                        onClick={() => setEntityId('')}
                        className="px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-50 text-gray-700"
                    >
                        查看所有问题
                    </button>
                </div>
            </div>

            {error && (
                <div className="bg-red-50 border-l-4 border-red-500 p-4 mb-6">
                    <div className="flex">
                        <AlertCircle className="h-5 w-5 text-red-500 mr-2" />
                        <p className="text-red-700">{error}</p>
                    </div>
                </div>
            )}

            {isLoading ? (
                <div className="flex justify-center items-center p-12">
                    <div className="flex flex-col items-center">
                        <svg className="animate-spin h-10 w-10 text-blue-500 mb-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                        </svg>
                        <p className="text-gray-600">加载代码质量数据...</p>
                    </div>
                </div>
            ) : (
                <>
                    {/* 质量评分 */}
                    {qualityScore !== null && (
                        <div className="bg-white shadow-sm rounded-lg p-6 mb-6">
                            <h3 className="text-lg font-medium text-gray-800 mb-4">代码质量评分</h3>

                            <div className="flex items-center">
                                <div className="w-32 h-32 rounded-full border-8 flex items-center justify-center relative"
                                     style={{
                                         borderColor: qualityScore >= 80 ? '#34D399' : qualityScore >= 60 ? '#FBBF24' : '#F87171',
                                         borderRightColor: 'transparent',
                                         transform: 'rotate(45deg)'
                                     }}>
                                    <div className="text-3xl font-bold" style={{ transform: 'rotate(-45deg)' }}>
                                        {qualityScore}
                                    </div>
                                </div>

                                <div className="ml-6">
                                    <h4 className="text-lg font-medium text-gray-800 mb-2">质量等级: {
                                        qualityScore >= 90 ? 'A' :
                                            qualityScore >= 80 ? 'B' :
                                                qualityScore >= 70 ? 'C' :
                                                    qualityScore >= 60 ? 'D' : 'F'
                                    }</h4>

                                    <div className="space-y-2">
                                        <div className="flex items-center">
                                            <span className="inline-block w-3 h-3 rounded-full bg-red-500 mr-2"></span>
                                            <span className="text-sm text-gray-600">错误: {qualityIssues.filter(issue => issue.severity === 'ERROR').length}</span>
                                        </div>
                                        <div className="flex items-center">
                                            <span className="inline-block w-3 h-3 rounded-full bg-yellow-500 mr-2"></span>
                                            <span className="text-sm text-gray-600">警告: {qualityIssues.filter(issue => issue.severity === 'WARNING').length}</span>
                                        </div>
                                        <div className="flex items-center">
                                            <span className="inline-block w-3 h-3 rounded-full bg-blue-500 mr-2"></span>
                                            <span className="text-sm text-gray-600">提示: {qualityIssues.filter(issue => issue.severity === 'INFO').length}</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* 质量问题列表 */}
                    <div className="bg-white shadow-sm rounded-lg p-6">
                        <h3 className="text-lg font-medium text-gray-800 mb-4">质量问题 ({qualityIssues.length})</h3>

                        {qualityIssues.length > 0 ? (
                            <div className="divide-y divide-gray-200">
                                {qualityIssues.map((issue, index) => (
                                    <div key={index} className="py-4">
                                        <div className="flex items-start">
                                            <div className="mt-1 mr-3">
                                                {getIssueTypeIcon(issue.type)}
                                            </div>
                                            <div className="flex-1">
                                                <div className="flex items-center mb-1">
                                                    <h4 className="text-md font-medium text-gray-800 mr-2">{issue.entityId}</h4>
                                                    <span className={`px-2 py-0.5 text-xs font-medium rounded-full ${getSeverityColor(issue.severity)}`}>
                            {issue.severity}
                          </span>
                                                </div>
                                                <p className="text-sm text-gray-700">{issue.message}</p>
                                                <p className="text-xs text-gray-500 mt-1">问题类型: {issue.type}</p>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <p className="text-gray-600">没有发现质量问题</p>
                        )}
                    </div>
                </>
            )}
        </div>
    );
}

export default CodeQualityPanel;
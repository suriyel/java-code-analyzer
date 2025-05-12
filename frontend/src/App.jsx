import React, { useState, useEffect } from 'react';
import { Search, Code, FileText, CheckCircle, AlertCircle, GitBranch, Database, BarChart2, Upload, Zap } from 'lucide-react';
import ProjectUploader from './components/ProjectUploader';
import CodeSearchPanel from './components/CodeSearchPanel';
import SemanticAnalysisPanel from './components/SemanticAnalysisPanel';
import CodeQualityPanel from './components/CodeQualityPanel';

// 主应用组件
export default function CodeAnalyzerApp() {
    const [activeTab, setActiveTab] = useState('upload');
    const [projectId, setProjectId] = useState('');
    const [projectStatus, setProjectStatus] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');

    // 根据activeTab渲染不同内容
    const renderContent = () => {
        switch (activeTab) {
            case 'upload':
                return <ProjectUploader setProjectId={setProjectId} setProjectStatus={setProjectStatus} setActiveTab={setActiveTab} />;
            case 'search':
                return <CodeSearchPanel projectId={projectId} />;
            case 'semantic':
                return <SemanticAnalysisPanel projectId={projectId} />;
            case 'quality':
                return <CodeQualityPanel projectId={projectId} />;
            default:
                return <ProjectUploader setProjectId={setProjectId} setActiveTab={setActiveTab} />;
        }
    };

    // 检查项目状态
    useEffect(() => {
        if (projectId) {
            const checkStatus = async () => {
                try {
                    setIsLoading(true);
                    const response = await fetch(`/api/v1/projects/${projectId}`);
                    const data = await response.json();
                    setProjectStatus(data.status);
                    setIsLoading(false);
                } catch (error) {
                    setErrorMessage('无法检查项目状态');
                    setIsLoading(false);
                }
            };

            checkStatus();
            // 如果状态是处理中，则每3秒检查一次
            if (projectStatus === 'PROCESSING') {
                const interval = setInterval(checkStatus, 3000);
                return () => clearInterval(interval);
            }
        }
    }, [projectId, projectStatus]);

    return (
        <div className="flex flex-col min-h-screen bg-gray-50">
            {/* 头部 */}
            <header className="bg-white shadow-sm border-b border-gray-200">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between h-16 items-center">
                        <div className="flex items-center">
                            <Code className="h-8 w-8 text-blue-600" />
                            <h1 className="ml-2 text-xl font-bold text-gray-900">Java代码解析与索引系统</h1>
                        </div>
                        {projectId && (
                            <div className="flex items-center text-sm text-gray-700">
                                <span className="mr-2">项目ID: {projectId}</span>
                                {projectStatus === 'READY' ? (
                                    <CheckCircle className="h-5 w-5 text-green-500" />
                                ) : projectStatus === 'PROCESSING' ? (
                                    <div className="flex items-center">
                                        <svg className="animate-spin h-5 w-5 text-blue-500 mr-1" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                        </svg>
                                        <span>处理中...</span>
                                    </div>
                                ) : (
                                    <AlertCircle className="h-5 w-5 text-red-500" />
                                )}
                            </div>
                        )}
                    </div>
                </div>
            </header>

            {/* 主内容区 */}
            <div className="flex-1 flex">
                {/* 侧边栏 */}
                <nav className="w-64 bg-white shadow-sm border-r border-gray-200">
                    <div className="h-full px-3 py-4 overflow-y-auto">
                        <ul className="space-y-2">
                            <li>
                                <button
                                    onClick={() => setActiveTab('upload')}
                                    className={`flex items-center w-full p-2 text-base rounded-lg ${activeTab === 'upload' ? 'bg-blue-100 text-blue-600' : 'text-gray-700 hover:bg-gray-100'}`}
                                >
                                    <Upload className="w-5 h-5" />
                                    <span className="ml-3">项目上传</span>
                                </button>
                            </li>
                            <li>
                                <button
                                    onClick={() => setActiveTab('search')}
                                    disabled={projectStatus !== 'READY'}
                                    className={`flex items-center w-full p-2 text-base rounded-lg ${activeTab === 'search' ? 'bg-blue-100 text-blue-600' : projectStatus !== 'READY' ? 'text-gray-400 cursor-not-allowed' : 'text-gray-700 hover:bg-gray-100'}`}
                                >
                                    <Search className="w-5 h-5" />
                                    <span className="ml-3">代码搜索</span>
                                </button>
                            </li>
                            <li>
                                <button
                                    onClick={() => setActiveTab('semantic')}
                                    disabled={projectStatus !== 'READY'}
                                    className={`flex items-center w-full p-2 text-base rounded-lg ${activeTab === 'semantic' ? 'bg-blue-100 text-blue-600' : projectStatus !== 'READY' ? 'text-gray-400 cursor-not-allowed' : 'text-gray-700 hover:bg-gray-100'}`}
                                >
                                    <GitBranch className="w-5 h-5" />
                                    <span className="ml-3">语义分析</span>
                                </button>
                            </li>
                            <li>
                                <button
                                    onClick={() => setActiveTab('quality')}
                                    disabled={projectStatus !== 'READY'}
                                    className={`flex items-center w-full p-2 text-base rounded-lg ${activeTab === 'quality' ? 'bg-blue-100 text-blue-600' : projectStatus !== 'READY' ? 'text-gray-400 cursor-not-allowed' : 'text-gray-700 hover:bg-gray-100'}`}
                                >
                                    <BarChart2 className="w-5 h-5" />
                                    <span className="ml-3">代码质量</span>
                                </button>
                            </li>
                        </ul>
                    </div>
                </nav>

                {/* 内容区 */}
                <main className="flex-1 p-6 overflow-auto">
                    {isLoading && activeTab !== 'upload' ? (
                        <div className="flex justify-center items-center h-full">
                            <div className="flex flex-col items-center">
                                <svg className="animate-spin h-10 w-10 text-blue-500 mb-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                </svg>
                                <p className="text-gray-600">加载中...</p>
                            </div>
                        </div>
                    ) : errorMessage ? (
                        <div className="bg-red-50 border-l-4 border-red-500 p-4 mb-4">
                            <div className="flex items-center">
                                <AlertCircle className="h-5 w-5 text-red-500 mr-2" />
                                <p className="text-red-700">{errorMessage}</p>
                            </div>
                        </div>
                    ) : (
                        renderContent()
                    )}
                </main>
            </div>
        </div>
    );
}
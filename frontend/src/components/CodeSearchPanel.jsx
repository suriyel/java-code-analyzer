import React, { useState } from 'react';
import { Search, AlertCircle } from 'lucide-react';

/**
 * 代码搜索面板 - 提供对代码的不同类型搜索
 */
function CodeSearchPanel({ projectId }) {
    const [searchQuery, setSearchQuery] = useState('');
    const [searchType, setSearchType] = useState('text');
    const [searchLevel, setSearchLevel] = useState('ALL');
    const [searchResults, setSearchResults] = useState([]);
    const [isSearching, setIsSearching] = useState(false);
    const [searchError, setSearchError] = useState('');

    const handleSearch = async (e) => {
        e.preventDefault();

        if (!searchQuery.trim()) {
            setSearchError('请输入搜索内容');
            return;
        }

        try {
            setIsSearching(true);
            setSearchError('');

            let url;
            if (searchType === 'text') {
                url = `/api/v1/projects/${projectId}/search?query=${encodeURIComponent(searchQuery)}&level=${searchLevel}`;
            } else if (searchType === 'semantic') {
                url = `/api/v1/projects/${projectId}/search/semantic?query=${encodeURIComponent(searchQuery)}`;
            } else if (searchType === 'relation') {
                const [relationType, target] = searchQuery.split(':');
                if (!target) {
                    setSearchError('关系搜索格式：关系类型:目标值 (例如 IMPLEMENTS:Serializable)');
                    setIsSearching(false);
                    return;
                }
                url = `/api/v1/projects/${projectId}/search/relation?relationType=${relationType.trim()}&target=${encodeURIComponent(target.trim())}`;
            }

            const response = await fetch(url);

            if (response.ok) {
                const data = await response.json();
                setSearchResults(data);
                if (data.length === 0) {
                    setSearchError('没有找到匹配的结果');
                }
            } else {
                const errorData = await response.json();
                setSearchError(errorData.message || '搜索失败');
            }
        } catch (error) {
            setSearchError('搜索过程中发生错误');
        } finally {
            setIsSearching(false);
        }
    };

    return (
        <div>
            <h2 className="text-xl font-semibold text-gray-800 mb-6">代码搜索</h2>

            <div className="bg-white shadow-sm rounded-lg p-6 mb-6">
                <form onSubmit={handleSearch} className="space-y-4">
                    <div className="flex space-x-4">
                        <div className="flex-1">
                            <label htmlFor="search-query" className="block text-sm font-medium text-gray-700 mb-1">搜索内容</label>
                            <input
                                id="search-query"
                                type="text"
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                placeholder={searchType === 'relation' ? "格式：IMPLEMENTS:Serializable" : "搜索代码或注释..."}
                                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-blue-500"
                            />
                        </div>

                        <div>
                            <label htmlFor="search-type" className="block text-sm font-medium text-gray-700 mb-1">搜索类型</label>
                            <select
                                id="search-type"
                                value={searchType}
                                onChange={(e) => setSearchType(e.target.value)}
                                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-blue-500"
                            >
                                <option value="text">全文搜索</option>
                                <option value="semantic">语义搜索</option>
                                <option value="relation">关系搜索</option>
                            </select>
                        </div>

                        {searchType === 'text' && (
                            <div>
                                <label htmlFor="search-level" className="block text-sm font-medium text-gray-700 mb-1">代码级别</label>
                                <select
                                    id="search-level"
                                    value={searchLevel}
                                    onChange={(e) => setSearchLevel(e.target.value)}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-blue-500"
                                >
                                    <option value="ALL">全部</option>
                                    <option value="FILE">文件</option>
                                    <option value="CLASS">类</option>
                                    <option value="METHOD">方法</option>
                                    <option value="FIELD">字段</option>
                                    <option value="SNIPPET">代码片段</option>
                                </select>
                            </div>
                        )}
                    </div>

                    {searchError && (
                        <div className="bg-red-50 border-l-4 border-red-500 p-4">
                            <div className="flex">
                                <AlertCircle className="h-5 w-5 text-red-500 mr-2" />
                                <p className="text-red-700">{searchError}</p>
                            </div>
                        </div>
                    )}

                    <div className="flex justify-end">
                        <button
                            type="submit"
                            disabled={isSearching}
                            className="flex items-center px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md"
                        >
                            {isSearching ? (
                                <>
                                    <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                    搜索中...
                                </>
                            ) : (
                                <>
                                    <Search className="h-4 w-4 mr-2" />
                                    搜索
                                </>
                            )}
                        </button>
                    </div>
                </form>
            </div>

            {/* 搜索结果 */}
            {searchResults.length > 0 && (
                <div className="bg-white shadow-sm rounded-lg p-6">
                    <h3 className="text-lg font-medium text-gray-800 mb-4">搜索结果 ({searchResults.length})</h3>

                    <div className="space-y-4">
                        {searchResults.map((result, index) => (
                            <div key={index} className="border border-gray-200 rounded-md p-4 hover:bg-gray-50">
                                <div className="flex justify-between items-start">
                                    <div>
                                        <h4 className="text-md font-semibold text-gray-800">{result.name}</h4>
                                        <p className="text-sm text-gray-600">{result.path}</p>
                                    </div>
                                    <span className="px-2 py-1 text-xs font-medium rounded-full bg-blue-100 text-blue-800">
                    {result.type}
                  </span>
                                </div>

                                {/* JavaDoc摘要 */}
                                {result.attributes.javadoc && (
                                    <div className="mt-2 text-sm text-gray-700">
                                        <p>{result.attributes.javadoc.length > 150
                                            ? result.attributes.javadoc.substring(0, 150) + '...'
                                            : result.attributes.javadoc}
                                        </p>
                                    </div>
                                )}

                                {/* 代码片段 */}
                                {result.attributes.snippet && (
                                    <div className="mt-2 p-2 bg-gray-50 rounded border border-gray-200 overflow-x-auto">
                                        <pre className="text-xs text-gray-800">{result.attributes.snippet}</pre>
                                    </div>
                                )}

                                {/* 其他属性 */}
                                <div className="mt-2 flex flex-wrap gap-2">
                                    {result.attributes.class && (
                                        <span className="px-2 py-1 text-xs rounded bg-gray-100 text-gray-800">
                      类: {result.attributes.class}
                    </span>
                                    )}
                                    {result.attributes.returnType && (
                                        <span className="px-2 py-1 text-xs rounded bg-gray-100 text-gray-800">
                      返回: {result.attributes.returnType}
                    </span>
                                    )}
                                    {result.attributes.fieldType && (
                                        <span className="px-2 py-1 text-xs rounded bg-gray-100 text-gray-800">
                      类型: {result.attributes.fieldType}
                    </span>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
}

export default CodeSearchPanel;
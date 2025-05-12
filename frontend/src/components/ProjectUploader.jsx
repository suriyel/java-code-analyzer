import React, { useState } from 'react';
import { Upload, AlertCircle } from 'lucide-react';

/**
 * 项目上传组件 - 处理Java项目ZIP文件的上传和分析
 */
function ProjectUploader({ setProjectId, setProjectStatus, setActiveTab }) {
    const [file, setFile] = useState(null);
    const [isDragging, setIsDragging] = useState(false);
    const [isUploading, setIsUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [uploadError, setUploadError] = useState('');

    const handleDragOver = (e) => {
        e.preventDefault();
        setIsDragging(true);
    };

    const handleDragLeave = () => {
        setIsDragging(false);
    };

    const handleDrop = (e) => {
        e.preventDefault();
        setIsDragging(false);

        if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
            const droppedFile = e.dataTransfer.files[0];
            if (droppedFile.name.endsWith('.zip')) {
                setFile(droppedFile);
                setUploadError('');
            } else {
                setUploadError('请上传ZIP格式的文件');
            }
        }
    };

    const handleFileChange = (e) => {
        if (e.target.files && e.target.files.length > 0) {
            const selectedFile = e.target.files[0];
            if (selectedFile.name.endsWith('.zip')) {
                setFile(selectedFile);
                setUploadError('');
            } else {
                setUploadError('请上传ZIP格式的文件');
            }
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!file) {
            setUploadError('请选择要上传的文件');
            return;
        }

        const formData = new FormData();
        formData.append('file', file);

        try {
            setIsUploading(true);
            setUploadProgress(0);

            // 模拟上传进度
            const progressInterval = setInterval(() => {
                setUploadProgress(prev => {
                    if (prev >= 90) {
                        clearInterval(progressInterval);
                        return 90;
                    }
                    return prev + 10;
                });
            }, 500);

            const response = await fetch('/api/v1/projects', {
                method: 'POST',
                body: formData,
            });

            clearInterval(progressInterval);
            setUploadProgress(100);

            if (response.ok) {
                const data = await response.json();
                setProjectId(data.projectId);
                setProjectStatus(data.status);
                setTimeout(() => {
                    setActiveTab('search');
                }, 1000);
            } else {
                const errorData = await response.json();
                setUploadError(errorData.message || '上传失败');
            }
        } catch (error) {
            setUploadError('上传过程中发生错误');
        } finally {
            setIsUploading(false);
        }
    };

    return (
        <div className="max-w-3xl mx-auto">
            <div className="bg-white shadow-sm rounded-lg p-6">
                <h2 className="text-xl font-semibold text-gray-800 mb-6">上传Java项目</h2>

                {uploadError && (
                    <div className="bg-red-50 border-l-4 border-red-500 p-4 mb-6">
                        <div className="flex">
                            <AlertCircle className="h-5 w-5 text-red-500 mr-2" />
                            <p className="text-red-700">{uploadError}</p>
                        </div>
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <div
                        className={`mb-6 border-2 border-dashed rounded-lg p-10 text-center cursor-pointer ${isDragging ? 'border-blue-500 bg-blue-50' : 'border-gray-300'}`}
                        onDragOver={handleDragOver}
                        onDragLeave={handleDragLeave}
                        onDrop={handleDrop}
                        onClick={() => document.getElementById('file-upload').click()}
                    >
                        <input
                            id="file-upload"
                            type="file"
                            accept=".zip"
                            className="hidden"
                            onChange={handleFileChange}
                            disabled={isUploading}
                        />

                        <div className="space-y-2">
                            <Upload className="h-10 w-10 text-gray-400 mx-auto" />
                            <p className="text-sm text-gray-600">
                                {file ? file.name : '拖拽或点击上传Java项目压缩包'}
                            </p>
                            <p className="text-xs text-gray-500">
                                支持ZIP格式，最大支持100MB
                            </p>
                        </div>
                    </div>

                    {isUploading && (
                        <div className="mb-6">
                            <div className="w-full bg-gray-200 rounded-full h-2.5">
                                <div
                                    className="bg-blue-600 h-2.5 rounded-full transition-all duration-300"
                                    style={{ width: `${uploadProgress}%` }}
                                ></div>
                            </div>
                            <p className="text-sm text-gray-600 mt-2 text-right">{uploadProgress}%</p>
                        </div>
                    )}

                    <div className="flex justify-end">
                        <button
                            type="submit"
                            disabled={!file || isUploading}
                            className={`flex items-center px-4 py-2 rounded-md ${!file || isUploading ? 'bg-gray-300 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700'} text-white`}
                        >
                            {isUploading ? (
                                <>
                                    <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                    上传中...
                                </>
                            ) : (
                                <>
                                    <Upload className="h-4 w-4 mr-2" />
                                    上传并分析
                                </>
                            )}
                        </button>
                    </div>
                </form>

                <div className="mt-8 border-t border-gray-200 pt-6">
                    <h3 className="text-lg font-medium text-gray-800 mb-4">使用说明</h3>
                    <ul className="list-disc pl-5 space-y-2 text-gray-600">
                        <li>上传包含Java源代码的ZIP压缩包</li>
                        <li>系统将自动解析代码结构，构建索引</li>
                        <li>分析完成后，可以使用代码搜索、语义分析和代码质量功能</li>
                        <li>对于大型项目，分析可能需要几分钟时间</li>
                    </ul>
                </div>
            </div>
        </div>
    );
}

export default ProjectUploader;
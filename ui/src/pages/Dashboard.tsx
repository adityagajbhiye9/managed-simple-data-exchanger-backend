import React, { SyntheticEvent, useState } from 'react';
import Nav from '../components/NavBar';
import Sidebar from '../components/Sidebar';
import UploadForm from '../components/UploadForm';
import { FileType } from '../models/FileType';
import { File } from '../models/File';

import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import Notification from '../components/Notification';
import dft from '../api/dft';
import UploadProgressBar from '../components/UploadProgressBar';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import CloseIcon from '@mui/icons-material/Close';
import StickyHeadTable from '../components/Table';

const Dashboard: React.FC = () => {
  const [isExpanded, setIsExpanded] = useState(false);
  const [menuIndex, setMenuIndex] = useState(0);
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [isDragging, setIsDragging] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [uploadProgress, updateUploadProgress] = useState(0);
  const [uploadStatus, setUploadStatus] = useState(false);
  const [uploading, setUploading] = useState(false);
  let dragCounter = 0;

  const handleExpanded = (expanded: boolean) => {
    setIsExpanded(expanded);
  };

  const validateFile = (file: File) => {
    const validTypes: string[] = Object.values(FileType);
    return validTypes.includes(file.type);
  };

  const handleFiles = (file: File) => {
    const maxFileSize = window._env_.FILESIZE;
    if (validateFile(file) && file.size < maxFileSize) {
      setSelectedFiles([...selectedFiles, file]);
    } else {
      file.invalid = true;
      setErrorMessage('File not permitted');
    }
  };

  const dragEnter = (e: any) => {
    e.preventDefault();
    e.stopPropagation();
    dragCounter++;
    if (e.dataTransfer.items && e.dataTransfer.items.length > 0) {
      setIsDragging(true);
    }
  };

  const dragLeave = (e: any) => {
    e.preventDefault();
    e.stopPropagation();
    dragCounter--;
    if (dragCounter > 0) return;
    setIsDragging(false);
  };

  const fileDrop = (e: any) => {
    e.preventDefault();
    const files = e.dataTransfer.files;
    if (files.length && files.length < 2 && selectedFiles.length === 0) {
      handleFiles(files[0]);
    } else {
      setErrorMessage('Only one file is permitted');
    }
    setIsDragging(false);
  };

  const removeSelectedFiles = (clearState: boolean) => {
    if (clearState) setSelectedFiles([]);
  };

  const getMenuIndex = (index = 0) => {
    setMenuIndex(index);
  };

  const uploadFile = (e: any) => {
    e.preventDefault();
    setUploading(true);
    const formData = new FormData();
    formData.append('file', selectedFiles[0] as any);

    dft
      .post('/upload', formData, {
        onUploadProgress: (ev: ProgressEvent) => {
          const progress = (ev.loaded / ev.total) * 100;
          updateUploadProgress(Math.round(progress));
        },
      })
      .then(resp => {
        console.log(resp.data);
        setTimeout(() => {
          setUploading(false);
          setUploadStatus(true);
        }, 1000);
      })
      .catch(err => console.error(err));
  };

  // TODO: Replace this logic with routes
  const layout = () => {
    if (menuIndex === 0) {
      return (
        <div className="flex flex-1 flex-col items-center justify-center min-w-0 relative">
          <div className="flex-[1_0_0%] flex order-1">
            <div className="flex flex-col items-center justify-center">
              {uploading ? <UploadProgressBar uploadProgress={uploadProgress} /> : null}
              {!uploading && (
                <UploadForm
                  getSelectedFiles={(files: any) => handleFiles(files)}
                  selectedFiles={selectedFiles}
                  removeSelectedFiles={removeSelectedFiles}
                  uploadStatus={uploadStatus}
                  emitFileUpload={(e: any) => uploadFile(e)}
                />
              )}
              {uploadStatus && (
                <div className="flex justify-between bg-[#e0eee0] p-4 w-full mt-4">
                  <div className="flex items-center gap-x-2">
                    <CheckCircleOutlineOutlinedIcon sx={{ color: 'rgb(34 197 94)' }} />
                    <p className="text-md">{selectedFiles[0].name}</p>
                  </div>
                  <span className="cursor-pointer" onClick={() => setUploadStatus(false)}>
                    <CloseIcon />
                  </span>
                </div>
              )}
            </div>
          </div>
        </div>
      );
    } else {
      return (
        <div className="flex-1 py-6 px-20">
          <h1 className="flex flex-row text-bold text-3xl">Upload History</h1>
          <div className="mt-8">
            <StickyHeadTable />
          </div>
        </div>
      );
    }
  };

  return (
    <div
      className="max-w-screen-4xl my-0 mx-auto overflow-y-auto overflow-x-hidden h-screen block"
      onDragOver={(e: SyntheticEvent) => e.preventDefault()}
      onDragEnter={dragEnter}
      onDragLeave={dragLeave}
      onDrop={fileDrop}
    >
      {!isDragging && (
        <main className="flex-1 flex flex-row justify-start min-h-screen pt-16 relative">
          <Nav getIsExpanded={(expanded: boolean) => handleExpanded(expanded)} />
          <div className="flex">
            <Sidebar isExpanded={isExpanded} emitMenuIndex={(index: number) => getMenuIndex(index)} />
          </div>
          {errorMessage !== '' && (
            <div className={`${isExpanded ? 'left-64' : 'left-14'} absolute top-16 z-50 w-screen`}>
              <Notification errorMessage={errorMessage} clear={() => setErrorMessage('')} />
            </div>
          )}

          <div className="flex w-screen">{layout()}</div>
        </main>
      )}

      {isDragging && (
        <div className="relative w-full h-full bg-[#03a9f4]">
          <div className="inset-x-0 inset-y-1/2 absolute z-5 flex flex-col justify-center gap-y-2 text-center">
            <span>
              <UploadFileOutlinedIcon style={{ fontSize: 60 }} sx={{ color: '#fff' }} />
            </span>
            <h1 className="text-4xl text-white">Drop it like it's hot :)</h1>
            <p className="text-lg text-white">Upload your file by dropping it in this window</p>
          </div>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
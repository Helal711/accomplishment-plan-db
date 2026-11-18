/// <reference types="vite/client" />

declare module '*.ttf' {
  const src: string;
  export default src;
}

declare module 'pdfmake/build/pdfmake' {
  const pdfMake: {
    addVirtualFileSystem: (vfs: Record<string, string>) => void;
    addFonts: (fonts: Record<string, {
      normal: string;
      bold?: string;
      italics?: string;
      bolditalics?: string;
    }>) => void;
    createPdf: (docDefinition: Record<string, unknown>) => {
      getBuffer: () => Promise<Uint8Array>;
      getBlob: () => Promise<Blob>;
    };
  };
  export default pdfMake;
}


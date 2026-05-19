import { saveAs } from "file-saver"

/**  
 * 下载图片  
 * @param url 图片下载地址  
 * @param fileName 要保存为的文件名  
 */  
export function downloadImage(url?: string, fileName?: string) {  
  if (!url) {  
    return  
  }  
  saveAs(url, fileName)  
}

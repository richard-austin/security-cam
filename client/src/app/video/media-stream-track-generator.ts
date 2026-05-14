// This MSTG is VIDEO ONLY, for use with Firefox (Nightly for Linux)
export let initMSTG=function()
{
  // @ts-ignore
 if (!window.MediaStreamTrackGenerator) {
   // @ts-ignore
   window.MediaStreamTrackGenerator = class MediaStreamTrackGenerator {
     constructor() {
       const canvas = document.createElement("canvas");
       const ctx = canvas.getContext('2d', {desynchronized: true});
       const track = canvas.captureStream().getVideoTracks()[0];
       // @ts-ignore
       track.writable = new WritableStream({
         write(frame) {
           canvas.width = frame.displayWidth;
           canvas.height = frame.displayHeight;
           ctx?.drawImage(frame, 0, 0, canvas.width, canvas.height);
           frame.close();
         }
       });
       return track;
     }
   }
 }
};


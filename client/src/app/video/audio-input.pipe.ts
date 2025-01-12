import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'audioInput',
    standalone: false
})
export class AudioInputPipe implements PipeTransform {

  transform(devInfo: MediaDeviceInfo[], ...args: unknown[]): MediaDeviceInfo[] {
    let retVal: MediaDeviceInfo[] = [];
    if(devInfo) {
      for (let i = 0; i < devInfo.length; ++i)
        if (devInfo[i].kind == "audioinput")
          retVal.push(devInfo[i])
    }
    return retVal;
  }
}

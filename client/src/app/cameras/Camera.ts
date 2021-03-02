
export enum uriType {lo="lo", hd="hd"};

export class Uri
{
    type!: uriType;
    uri!:string;
}
export class Camera
{
    name: string = "";
    motionName: string = "";
    uris: Uri[] = [];
    recordings: Uri[] = [];
}

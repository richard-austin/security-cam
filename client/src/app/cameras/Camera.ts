
export enum uriType {lo="lo", hd="hd"};

export class Uri
{
    type!: uriType;
    uri!:string;
}
export class Camera
{
    name: string = "";
    uris: Uri[] = [];
    recordings: Uri[] = [];
}

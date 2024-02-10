package main

var bbfMarshallPoint BBFMarshalling

type BBFMarshalling struct {
	bbf []*BucketBrigadeFeeder
}

func (cb BBFMarshalling) Init() {
	cb.bbf = make([]*BucketBrigadeFeeder, 0)
}

package main

var callbacks Callbacks

type Callbacks struct {
	bbf []*BucketBrigadeFeeder
}

func (cb Callbacks) Init() {
	cb.bbf = make([]*BucketBrigadeFeeder, 0)
}

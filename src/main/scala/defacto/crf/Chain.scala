package defacto.crf

import scala.io.Source
import scala.collection.mutable.ArrayBuffer
class Chain(val weights : Weights, val ff : (String=>Array[Int])) extends Iterable[Clique] {
	var end : Label = null
	var startClique : Clique = null
	var cliqueSize : Int = 0

  def iterator = new Iterator[Clique] {
    var current : Clique = startClique
    def next() = { val r = current; current = current.next; r }
    def hasNext = current != null
  }

  def labelIterator = new Iterator[Label] {
    var firstHasNext = 0
    val cliqueIterator = iterator
    var current : Clique = null
    def next() = {
    if(firstHasNext == 1) {
      firstHasNext += 1
      current.transFactor.right
    } else {
      current = cliqueIterator.next()
      if(!cliqueIterator.hasNext) firstHasNext += 1
      current.transFactor.left
    }}
    def hasNext = iterator.hasNext && firstHasNext < 2
  }

	// Load chain takes in a sentence file and compiles a CRF chain
	def loadChain(file : String) : Chain = {
		var prev : Label = null
		var count = 0
		for(line <- Source.fromFile(file).getLines()) {
			val split = line.split(",")
			val obs = new Observation(count, split(0), ff(line))
			val lab = new Label(count, split(1))
			new ObservationFactor(obs, lab, weights)
			if(prev != null) new TransitionFactor(prev, lab, weights)
			prev = lab
			count += 1
		}
		end = prev
		this
	}

  def loadClassifierChain(line : String) : Chain = {
    val split = line.split(",")
    val obs = new Observation(0, split(1), ff(line))
    val lab = new Label(0, split(0))
    new ObservationFactor(obs, lab, weights)
    end = lab
    this
  }

  def print() {
		var leftFactor : TransitionFactor = end.leftFactor
		while(leftFactor != null) {
			println("Factor: " + leftFactor.y)
			for(i <- 1 to weights.labels.until) {
				for(j <- 1 to weights.labels.until) {
					println(leftFactor(i,j))
				}
			}
			leftFactor = leftFactor.left.leftFactor
			println("")
		}
		var obsFactor : ObservationFactor = end.observationFactor
		while(obsFactor != null) {
			println("Obs Factor: " + obsFactor.index)
			for(i <- 1 to weights.labels.until) {
				println(obsFactor(i))
			}
			if(obsFactor.label.leftFactor != null)
				obsFactor = obsFactor.label.leftFactor.left.observationFactor
			else
				obsFactor = null
			println("")
		}
		
	}

  def singleClique(): Unit = {
    startClique = new Clique(weights.labels.until, 0)
    cliqueSize = 1
    startClique.addFactor(end.observationFactor)
  }
	
	def makeCliqueTree() {
		var pointer = end.leftFactor
		var prev : Clique = null
		var first = true
		var count = 0
		var clique : Clique = null
		while(pointer != null) {
			clique = new Clique(weights.labels.until, count+1)
			if(first) {
				first = false
				clique.addFactor(pointer.right.observationFactor)
			}
			clique.addFactor(pointer.left.observationFactor)
			clique.addFactor(pointer)
			clique.next = prev
			if(prev != null) prev.prev = clique
			prev = clique
			pointer = pointer.left.leftFactor
			count += 1
		}
		startClique = clique
    cliqueSize = count
		count = 0
	  var current : Clique = clique
		while(current != null) {
			current.index = count
			current = current.next
			count += 1
		}
	}
	
	def computeCliques() {
		var current : Clique = startClique
		while(current != null) {
			current.compute()
			current = current.next
		}
	}

  def logComputeCliques() {
    var current : Clique = startClique
    while(current != null) {
      current.logCompute()
      current = current.next
    }
  }
} 


out = java.lang.System.out

var bigIntClass = java.math.BigInteger

function toBigInt(x) {
  if (typeof x == 'number') {
    return new java.math.BigInteger(x)
  }
  if (typeof x == 'object' && x.getClass() == bigIntClass) {
    return x
  }
  //return null
}

function createEnc(enckey) {
  var e = function(m) {
    return enckey.encrypt(toBigInt(m))
  }
  e.key = enckey
  e.add = function(x,y) {
    return enckey.add(x,y)
  }
  return e
}

function createDec(deckey) {
  var d = function(m) {
    return deckey.decrypt(toBigInt(m))
  }
  d.key = deckey
  return d
}
function genpair() {
  var deckey = Packages.sfe.crypto.Paillier.genKey(256)
  var enckey = deckey.encKey()
  
  var e = createEnc(enckey)
  var d = createDec(deckey)
  d.e = e
  
  return {enc: e, dec: d}
}

function send(who, obj) {
  commParty.send(obj)
}

function recv(who) {
  return commParty.recv()
}
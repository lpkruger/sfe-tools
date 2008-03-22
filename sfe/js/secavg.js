if (commPartyName=="alice") {
 function secavg(x,m) {
    keys = genpair()
    e=keys.enc
    d=keys.dec
    send("bob", e)
    send("bob", e(x))
    send("bob", e(m))
    en = recv("bob")
    ed = recv("bob")
    val = d(en).divide(d(ed))
    send("bob", val)
    return val
  }
} else if (commPartyName=="bob") {
  function secavg(y,n) {
    e = recv("alice")
    ex= recv("alice")
    em= recv("alice")
    
    en = e.add(ex, e(y))
    ed = e.add(em, e(n))
    send("alice", en)
    send("alice", ed)
    val = recv("alice")
    return val
  }
}

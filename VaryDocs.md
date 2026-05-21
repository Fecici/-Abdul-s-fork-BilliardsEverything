    This document attempts to explain the idea behind the VaryCS and Vary3 used by Vary, and provides an approximation of the algorithms which focusses on understandability. The actual implementation details are not the focus, and the interested programmer should read the code in the files VaryCS.java, Vary3.java, and TriangleBilliards.java. 

    Let a, b be the vary x and y coordinates, converted to radians. They correspond to the triangle with the vertices A = a, B = b, C = pi - a - b and sides AB, AC, BC with the side lengths: 
    |BC| = sin(a), 
    |AC| = sin(b), 
    |AB| = sin(c) = sin(pi - a - b) = sin(a + b). 

VaryCS:

    To start off, we search for only CS codes using a special algorithm VaryCS.fireAway. Given the triangle ABC and the arbitrary starting side AB, we first align the side AB with the horizontal axis such that A is at (0, 0), B is at (|AB|, 0), and C is at (|AC|*cos(a), |BC|*sin(b)). We know that CS codes contain a 90 degree reflection, and that a code sequence, being a periodic path, can be rotated. Thus, we can set the first shooting angle off of side AB to be 90 degrees, or straight upwards. This has the benefit of allowing us to represent the band of valid poolshot vectors as an open interval (l, r) denoting the x position of the leftmost and rightmost poolshot vector respectively. In the begining, we initialize these as l = 0, r = |AB|. In addition, since we have no reflections yet, the side sequence corresponding to this state is the empty sequence "". This is the initial state of our recursion.

    Now, suppose we have an arbitrary state after some number of reflections >= 0, with the vertice coordinates X Y Z, and the beam interval (l, r).Suppose that XY is the base, or the previous reflected side. We first check if the band described by the interval (l, r) gets split by Z. This occurs if l < Z.x < r, in which case we recursively branch into two states, a reflection of part of the beam across XZ (the left non-base edge) and a seperate reflection across YZ (the right non-base edge). These two states will have bands with intervals (l, Z.x), (Z.x, r) and coordinates X Z Y', Z Y X' respectively (where Y' is the point Y reflected across XZ, and X' is the point X reflected across YZ). Each time we create a new state, we can easily calculate the side sequence pf this state, by appending the side we reflected off to the side sequence corresponding to the parent state. (The starting state corresponds to an empty side seq). 

    If instead Z.x <= l, it is only possible to reflect across YZ, and there is only one new state, with band interval (l, r) and vertice coordinates Z Y X'. the band in this new state will remain as (l, r). Likewise, if r <= C.x, it is only possible to reflect across AC, and the band in this new state will also remain as (l, r).

    This same process can then be applied to the new states we have created, until we have reached the maximum side sum size for our codes. If at any point the side sequence is a periodic path, we can then add it to the set of periodic paths for this triangle. We must repeat this process for each of the triangle's 3 sides, since the 90 degree reflection can occur against any side. Note that checking whether a code is periodic is done by checking that the sidesum is roughly 0, and that |r-l| > eps, where eps is some small value. The following is a formal recurrance describing the process above.

    let g be a function which takes a code returns the set containing the passed code if it is periodic, and the empty set if not
    g(code) = {
        {code} code is periodic,
        {} code is not periodic
    }
    where {} is the empty set

    Now, let f(s, d, l, r, X, Y, Z, codeCurr) be a recurrance 
    where s is the max side sum, 
    d is the current recursion depth, 
    l is the left x position of the valid poolshot band, 
    r is the right x position of the valid poolshot band,
    X is the coordinate of the left base vertex, 
    Y is the coordinate of the right base vertex,
    Z is the coordinate of the non-base vertex,
    codeSet is the set of all periodic CS codes found,
    codeCurr is the side sequence up to this point

    f(s, d, l, r, X, Y, Z, codeCurr) = {
        g(codeCurr); (d >= s)
        g(codeCurr) U f(s, d+1, l, Z.x, X, Z, Y', codeCurr + XZ) U f(s, d+1, Z.x, r, Z, Y, X', codeCurr + YZ); (l < Z.x < r)
        g(codeCurr) U f(s, d+1, l, r, Z, Y, X', codeCurr + YZ); (Z.x <= l)
        g(codeCurr) U f(s, d+1, l, r, X, Z, Y', codeCurr + XZ); (r <= Z.x)
    }
    Where U is the set union operation.

    We query the starting state as f(s, 0, 0, |AB|, A, B, C, "")

 
Vary3:

    Let's examine the general vary algorithm. Let n be the number of shots to take. We will shoot from side AB, and our shots will start along this side at positions |AB|/(n + 1), ... (n-1)*|AB|/(n+1), n*|AB|/(n+1) (With A at position 0, and B at position |AB|). For a single shot, let pos be the position of that shot. We will align ABC with the x-axis such that A is at (-pos, 0), B is at (|AB| - pos, 0), and the shot starts at (0,0). We also construct the triangle such that C is above the x axis (This is always possible). Since unlike with CS we do not have an angle which we can fix, we define a range of possible shooting angles from our starting point minAngle = 0, maxAngle = pi. Together, the coordinates of our two base vertices A, B, the coordinate of our non-base vertice C, and our range of shooting angles (minAngle, maxAngle) defines the starting state.

    Now, given an arbitrary state with base vertices X, Y, non-base vertice Z, and range of angles (minAngle, maxAngle), we first calculate specAngle = arctan(Z.y/Z.x), which is the angle from the start of the shot (the origin) to the vertice Z. if minAngle < specAngle < maxAngle, then the range of shot angles from (minAngle, specAngle) will reflect off of side YZ, and the range of angles from (specAngle, maxAngle) will reflect off of side XZ. Thus, we recursively branch into two states, one with base vertices X, Z, non-base vertice Y', and range of angles (specAngle, maxAngle), and the other with base vertices Y, Z, non-base vertice X', and range of angles (minAngle, specAngle). If maxAngle <= specAngle, then there is only a single recursive state, with base vertices Y, Z, non-base vertice X', and range of angles (minAngle, maxAngle), while if minAngle >= specAngle the following state has base vertices X, Z, non-base vertice Y', and range of angles (minAngle, maxAngle). 

    Like with VaryCS, we track the side sequence associated to each state, and whenever we detect that a sequence is periodic, we add it to the set of periodic paths. This can find paths of all types, but is much slower than VaryCS.

    Vary4 is a different vary algorithm designed to find all possible codes at a given coordinate. Unlike Vary3, it does not shoot from a single starting point, and as a result it is unaffected by the number of shots (just like VaryCS).

    In this documentation, I will use A, B, C, to denote the vertices of the currently active triangle. AB is the side we shoot from, and C will always refer to the non-base vertice, which splits our beam.

    In our calculations, we will make use of worst lines. Given a single fixed point and a set of different points, the worst line is a line which passes through the fixed point and some point in the set, such that the absolute slope of the line is maximized.

    Given the vary coordinates (x, y) in radians, our initial triangle will have A = (0,0), B = (sin(x + y), 0), and C = (sin(y)*cos(x), sin(y)*sin(x)). This is the same as how coordinates were initially assigned for Vary3, and has AB along the x-axis with vertex C above. We will also track two arrays L and R, which are "trails" of relevent vertices along the left and right side of our unfolding. L = {A}, and R = {B}. Our beam will be from 0 to pi radians to start. These angles correspond to the angles of the worst line between A and R and the worst line between B and L. This is the initial state of our recurrance.

    When given an arbitrary state in our unfolding, we can either unfold to the left or the right. Both directions work in very similar ways, so we focus just on going left. To go left, we first check that the left side is actually within the bounds of our beam. We do this by finding the worst line from vertex C to the points in R, and seeing if the angle of this line is less than the max of our beam. Here, the worst line corresponds to the minimum shooting angle required to see the left side from somewhere on the base, and it must be within our beam if we are to go left. 

    It is easy to reflect the triangle across its edge. Since we are reflecting across the left, vertex C will be on the right of the unfolding. As a small optimization, we also calculate the angle of the worst line between vertex C and R, and we only append C to R if the angle is greater than our minimum beam angle. If the angle of the worst line is less than our minimum angle, then that means we would be unable to see C from the base of our unfolding, so there is no point in including it in the list R.

    We must also recalculate the bounds of our beam. When we go left, only the minimum shooting angle can change. The new minimum shooting angle will be the minumum of the worst line between vertex C and R, and the previous minumum shooting angle. After we have calculated this, we can now recurse with the reflected triangle as our starting state. 

    To check that we have reached a periodic path, we store a record of all the sides we have encountered so far. Just as with Vary3, we check that the final triangle is parallel and has the same orientation with our starting triangle. We then calculate the angle between vertex A of the final triangle, and vertex A of the starting triangle. 

    This must be the shooting angle of the periodic shot, if that shot exists. If this shooting angle is between the minimum and maximum angles of our beam, then a periodic path certainly exists. This is because we get the minimum and maximum angles from worst lines, which lay completely inside the poolshot tower. Further, the worst lines corresponding to the min and max angles intersect within the tower, so if we have our shot pass through this intersection as well, then our shot will also lie completely inside the poolshot tower.



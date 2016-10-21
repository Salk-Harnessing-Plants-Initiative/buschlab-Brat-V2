package at.ac.oeaw.gmi.brat.segmentation.algorithm.chowkaneko;

import java.awt.geom.Point2D;
import java.util.AbstractList;
import java.util.ArrayList;


/**
 * Classe de fonctions mathématiques
 * @author VV
 */
public class CMaths {
	
	/**
	* Renvoie l'angle formé par les écarts dx et dy
	*/
	static double getAngleDxDy( double nDeltaX, double nDeltaY ) {
		double nAngle;

		//Cas particulier pour fonction Atn (Atan en C)
		if ( nDeltaX == 0 ) {
			if ( nDeltaY < 0 )
				nAngle = -Math.PI / 2.0;
			else
				nAngle = Math.PI / 2.0;
		}

		//Cas "Normaux"
		else {
			nAngle = Math.atan( nDeltaY / nDeltaX );
			if ( nDeltaX < 0.0 )
				nAngle = nAngle + Math.PI;
		}

		//Récupération d'un résultat positif
		if ( nAngle < 0.0 )
			nAngle = nAngle + 2.0 * Math.PI;
		return nAngle;
	}// getAngleDxDy
	
	
	/**
	* Renvoie la valeur arrondie de nVal avec le nombre de
	*  décimales demandé
	*/
	static int arrondir(double nNombre ) {
		return (int)arrondir( nNombre, 0 );
	}
	static double arrondir(double nNombre, int nNbDecimales ) {
		if ( nNbDecimales <= 0 ) {
			return Math.floor(nNombre + 0.5);
		}
		double nPrecision = Math.pow(10, nNbDecimales);
		return Math.floor(nNombre * nPrecision + 0.5) / nPrecision;
	}// arrondir
	
	
	/**
	 * Renvoie la différence min entre les angles 1 et 2
	 */
	static double getDifference_min_entre_angles_rad( double nAngle_rad_1, double nAngle_rad_2 ){
		double nDiff1, nDiff2;
		nDiff1 = Math.abs( nAngle_rad_1 - nAngle_rad_2 );
		nDiff2 = Math.abs( nDiff1 - Math.PI);
		return Math.min( nDiff1, nDiff2 );
	}// getDifference_min_entre_angles_rad
	
	
	/**
	 * Calcule une interpolation d'Hermite entre une série de points
	 * ( vPts_ctrl sont des points issus d'une fonction continue, dérivable )
	 * Renvoie un polygone avec la densité de points demandée
	 * @param vPts_crtl
	 * @param nNb_pts_par_unite
	 * @return vPoly_Hermite : Une liste de points interpolés
	 */
	static public AbstractList getPoly_Hermite( AbstractList vPts_crtl, int nNb_pts_par_unite ) {
		AbstractList vPoly_Hermite = new ArrayList();
		int nNb = vPts_crtl.size();
		if ( nNb <= 2 ) {
			return vPoly_Hermite;// pas assez de points pour interpoler
		}
		double xMin = ((Point2D.Double)vPts_crtl.get(0)).x;
		double xMax = ((Point2D.Double)vPts_crtl.get(nNb - 1)).x;
		if ( xMin >= xMax ) {
			return vPoly_Hermite;// pas assez d'espace pour interpoler
		}

		// Réservation de la place pour les tableaux
		double[] x = new double[ nNb ];
		double[] y = new double[ nNb ];
		double[] pente = new double[ nNb ];
		
		// Mémorisation
		int nI = 0;
		while ( nI < nNb ) {
			x[nI] = ((Point2D.Double)vPts_crtl.get(nI)).x;
			y[nI] = ((Point2D.Double)vPts_crtl.get(nI)).y;
			pente[nI] = -1;
			// suiv
			++nI;
		}

		// Calcul des pentes en i
		nI = 1;
		while( nI < nNb - 1 ) {
			double deltaX = x[nI + 1] - x[nI - 1];
			double deltaY = y[nI + 1] - y[nI - 1];
			if ( deltaX != 0 ) {
				pente[nI] = deltaY / deltaX;
			}
			// suiv 
			++nI;
		}
		
		// Calcul des pentes en début et en fin de courbe
		// - début
		double deltaX = x[1] - x[0];
		double deltaY = y[1] - y[0];
		if ( deltaX != 0 ) {
			pente[0] = deltaY / deltaX;
		}
		// - fin
		deltaX = x[nNb - 1] - x[nNb - 2];
		deltaY = y[nNb - 1] - y[nNb - 2];
		if ( deltaX != 0 ) {
			pente[nNb - 1] = deltaY / deltaX;
		}
		// Calcul du polynôme entre i et i + 1 et ajout des valeurs
		double dx = 1.0 / (double)nNb_pts_par_unite;
		nI = 0;
		while ( nI < (nNb - 1) ) {
			double x0 = x[nI];
			double x1 = x[nI + 1];
			double y0 = y[nI];
			double y1 = y[nI + 1];
			double p0 = pente[nI];
			double p1 = pente[nI + 1];
			double xPixel0 = x0;
			double xPixel1 = x1;
			// ajout des points d'interpolation dans la série
			double xPixel = xPixel0;
			while ( xPixel < ( xPixel1 - dx + 0.000001 ) ) {
				double xp = xPixel;
				double phi0 = ( (xp-x1)*(xp-x1) ) / 
								  ( (x1-x0)*(x1-x0)*(x1-x0) ) * 
								  ( 2*(xp-x0)+(x1-x0) );
				double phi1 = ( (xp-x0)*(xp-x0) )/  
								  ( (x0-x1)*(x0-x1)*(x0-x1) ) *  
								  ( 2*(xp-x1)+(x0-x1) );
				double phi2 = ( (xp-x0)*(xp-x1)*(xp-x1) ) /  
								  ( (x0-x1)*(x0-x1) );
				double phi3 = ( (xp-x1)*(xp-x0)*(xp-x0) ) /  
								  ( (x1-x0)*(x1-x0) );
				double yp = y0 * phi0 + y1 * phi1 + p0 * phi2 + p1 * phi3;
				vPoly_Hermite.add( new Point2D.Double( xp, yp ) );
				//
				xPixel = xPixel + dx;
			}
			// suiv
			nI = nI + 1;
		}
		// ajout du dernier point de controle
		vPoly_Hermite.add( (Point2D.Double)vPts_crtl.get( nNb - 1 ) );
		
		return vPoly_Hermite;
	}// getPoly_Hermite
	
	
	/**
	 * Calcule une interpolation par spline naturelle cubique entre une 
	 *   série de points
	 * Voir aussi spline "cardinale" (passe par les points interpolés)
	 * Les splines sont des courbes C2 (continuité des dérivées premières et secondes).
	 * Renvoie un polygone avec la densité de points demandée
	 * @param vPts_crtl points issus d'une fonction continue, dérivable
	 * @param nNb_pts_par_unite
	 * @return vPoly_Hermite : Une liste de points interpolés
	 */
	static public AbstractList getSpline_naturelle( AbstractList vPts_crtl, int nNb_pts_par_unite ) {
		AbstractList vPoly_spline = new ArrayList();
		int nNb = vPts_crtl.size();
		if ( nNb <= 2 ) {
			return vPoly_spline;// pas assez de points pour interpoler
		}
		double xMin = ((Point2D.Double)vPts_crtl.get(0)).x;
		double xMax = ((Point2D.Double)vPts_crtl.get(nNb - 1)).x;
		if ( xMin >= xMax ) {
			return vPoly_spline;// pas assez d'espace pour interpoler
		}

		// Réservation de la place pour les tableaux
		double x[] = new double[nNb];
		double y[] = new double[nNb];
		// Mémorisation
		for (int nI  = 0; nI < nNb; ++nI) {
			x[nI] = ((Point2D.Double)vPts_crtl.get(nI)).x;
			y[nI] = ((Point2D.Double)vPts_crtl.get(nI)).y;
		}
		// Etape 1 : calcul des distances horizontales entre les points
		double h[] = new double[nNb];
		for ( int nI = 0; nI <= nNb - 2; ++nI) {
			h[nI] = x[nI + 1] - x[nI];
		}
		// Etape 2 : calcul des pentes entre les points successifs
		double m[] = new double[nNb];
		for (int nI = 0; nI <= nNb - 2; ++nI ) {
			m[nI] = ( y[nI + 1] - y[nI] ) / h[nI];// h[nI] est toujours != 0
		}
		// Etape 3 : calcul des vecteurs u et v
		double u[] = new double[nNb];
		double v[] = new double[nNb];
		u[0] = 1;
		v[0] = 0;
		for (int nI = 1; nI <= nNb - 2; ++nI) {
			u[nI] = 2 * ( h[nI] + h[nI - 1] );
			v[nI] = 6 * ( m[nI] - m[nI - 1] );
		}
		u[nNb - 1] = 1;
		v[nNb - 1] = 0;
		// Etape 4 : résolution du système tridiagonal
		// (utilisation du code du livre "Numerical recipes" 2.4, page 51
		double z[] = new double[nNb];// inconnues
		// diagonale principale = u[]
		double a[] = new double[nNb];// diagonale en dessous de u
		double c[] = new double[nNb];// diagonale au dessus  de u
		// - - remplissage des diagonales a et c, u est déjà remplie
		a[0] = 0;
		c[0] = 0;
		for ( int nI = 1; nI <= nNb - 2; ++nI ) {
			a[nI] = h[nI - 1];
			c[nI] = h[nI];
		}
		a[nNb - 1] = 0;
		c[nNb - 1] = 0;
		// - - Numerical recipes
		double gam[] = new double[nNb];
		double bet = u[0];
		z[0] = v[0] / bet;
		for ( int nI = 1; nI <= nNb - 1; ++nI ) {
			gam[nI] = c[nI - 1] / bet;
			bet = u[nI] - a[nI] * gam[nI];
			z[nI] = ( v[nI] - a[nI] * z[nI - 1] ) / bet;
		}
		for (int nI = (nNb - 2); nI >= 0; --nI ) {
			z[nI] -= gam[nI + 1] * z[nI + 1];// solutions
		}

		// Etape 5 : calcul des coefficients de la spline
		a = new double[nNb];
		double b[] = new double[nNb];
		c = new double[nNb];
		double d[] = new double[nNb];
		for (int nI = 0; nI <= nNb - 2; ++nI ) {
			a[nI] = ( z[nI + 1] - z[nI] ) / (6 * h[nI]);
			b[nI] = z[nI] / 2;
			c[nI] = ( (y[nI + 1] - y[nI])/ h[nI] ) -
					( (2 * h[nI] * z[nI] + h[nI] * z[nI + 1]) / 6 );
			d[nI] = y[nI];
		}
		// Etape 6 : Calcul du polynôme entre i et i + 1 et ajout des valeurs
		for (int nI = 0; nI < (nNb - 1); ++nI ) {
			double xPixel0 = x[nI];
			double xPixel1 = x[nI + 1];
			double dx = 1.0 / ( double )nNb_pts_par_unite;
			for ( double xPixel = xPixel0; xPixel <= (xPixel1 - dx); xPixel += dx ) {
				double xp = xPixel;
				// Calcul de yp en fonction de xp
				double yp =  a[nI] * (xp - x[nI]) * (xp - x[nI]) * (xp - x[nI]) +
						b[nI] * (xp - x[nI]) * (xp - x[nI]) +
						c[nI] * (xp - x[nI]) +
						d[nI];
				vPoly_spline.add( new Point2D.Double( xp, yp ) );
			}// for
		}// for
		// ajout du dernier point de controle
		vPoly_spline.add( (Point2D.Double)vPts_crtl.get( nNb - 1 ) );

		// Renvoi de la spline
		return vPoly_spline;
	}// getSpline_naturelle
}// CMaths

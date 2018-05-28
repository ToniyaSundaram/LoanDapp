package com.example.api


import com.example.flow.ConfrimFlow.ConfrimLender
import com.example.flow.LendRequestFlow.Lender
import com.example.flow.LoanRequestFlow.Initiator
import com.example.state.LoanState
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.internal.x500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.core.Response.Status.CREATED

val SERVICE_NAMES = listOf("Notary", "Network Map Service")

// This API is accessible from /api/loan. All paths specified below are relative to it.
@Path("loan")
class ExampleApi(private val rpcOps: CordaRPCOps) {
    private val myLegalName: CordaX500Name = rpcOps.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger: Logger = loggerFor<ExampleApi>()
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = rpcOps.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }


    /**
     * Displays all LoanRequest states that exist in the node's vault.
     */
    @GET
    @Path("loanRequests")
    @Produces(MediaType.APPLICATION_JSON)
    fun getLoanrequests(): List<LoanState> {
        val loanStateandRef = rpcOps.vaultQueryBy<LoanState>().states
        val loanStates =loanStateandRef.map { it.state.data }
        return loanStates
    }



    /**
     * Initiates a flow to requestLoan and send it to all parties.
     *
     * Once the flow finishes it will write the loan to ledger. The borrower and all the lenders will be able to
     * see it when calling /api/loan/loan_request on their respective nodes.
     *
     * This end-point takes loanAmount, interestRate and lenders as parameters
     * Lenders parameter are the other parties in the node. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @PUT
    @Path("loan_request")
    fun createLoan(@QueryParam("loan_amount") loanAmount: Int, @QueryParam("interest_rate") interestRate: Int,
                  @QueryParam("lenders") lenders: List<String>): Response {
        if (loanAmount <= 0 ) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'loanAmount' must be non-negative.\n").build()
        }
        println("myLegalName"+myLegalName.x500Name)
        val thisNode = myLegalName.x500Name

        if(thisNode.equals("PartyA")) {
            print("Yes I am partyA")
        }
        val lenderList = ArrayList<Party>()

        /**Here in this for loop we iterate through the list of lenders and check whether they are valid.
         *If its valid,  it adds the valid lender name in  the list and initiates the flow
         */

        for(i in lenders.indices) {
            if(lenders[i].equals("PartyA")) {
                println("Inside PartyA ")
                val lenderName = rpcOps.wellKnownPartyFromX500Name(CordaX500Name("PartyA", "London", "GB")) ?:
                return Response.status(BAD_REQUEST).entity("Party named $lenders[i] cannot be found.\n").build()
                lenderList.add(lenderName)
            }else if(lenders[i].equals("PartyB")) {
                println("Inside PartyB ")
                val lenderName = rpcOps.wellKnownPartyFromX500Name(CordaX500Name("PartyB", "New York", "US")) ?:
                return Response.status(BAD_REQUEST).entity("Party named $lenders[i] cannot be found.\n").build()
                lenderList.add(lenderName)
            }else if(lenders[i].equals("PartyC")) {
                println("Inside PartyC ")
                val lenderName = rpcOps.wellKnownPartyFromX500Name(CordaX500Name("PartyC", "Paris", "FR")) ?:
                return Response.status(BAD_REQUEST).entity("Party named $lenders[i] cannot be found.\n").build()
                lenderList.add(lenderName)
            }else {
                return Response.status(BAD_REQUEST).entity("Party named $lenders[i] cannot be found.\n").build()
            }

        }
        return try {
            val signedTx = rpcOps.startTrackedFlow(::Initiator, loanAmount, interestRate, lenderList).returnValue.getOrThrow()
            Response.status(CREATED).entity("Transaction id ${signedTx.id} committed to ledger.\n").build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }

    /**
     * Initiates a flow to lendRequest and send it to the borrower
     *
     * Once the flow finishes it will write the lend information to ledger. The borrower and all the lender who submits the request will only be able to
     * see it when calling /api/loan/lend_request on their respective nodes.
     *
     * This end-point takes loanAmount, interestRate  as parameters
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @PUT
    @Path("lend_request")
    fun lendLoan(@QueryParam("loan_amount") loanAmount: Int,
                  @QueryParam("interest_rate") interestRate: Int): Response {

        val acceptorList = ArrayList<Party>()
        return try {
            val accpetor = rpcOps.wellKnownPartyFromX500Name(CordaX500Name("PartyA", "London", "GB")) ?:
            return Response.status(BAD_REQUEST).entity("You are not a valid acceptor\n").build()
            acceptorList.add(accpetor)

            val accpetor1= rpcOps.wellKnownPartyFromX500Name(CordaX500Name("PartyC", "Paris", "FR")) ?:
            return Response.status(BAD_REQUEST).entity("You are not a valid acceptor\n").build()
            acceptorList.add(accpetor1)

            val status= "InAuction"

            val flowHandle = rpcOps.startTrackedFlow(::Lender, loanAmount,interestRate,status,acceptorList)
            flowHandle.progress.subscribe { println(">> $it") }

            // The line below blocks and waits for the future to resolve.
            val result = flowHandle.returnValue.getOrThrow()

            Response.status(CREATED).entity("Transaction id ${result.id} committed to ledger.\n").build()

        }catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }

    }

    /**
     * Initiates a flow to confirm lender and send to the lender
     *
     * Once the flow finishes it will write the lend information to ledger. The borrower and all the lender who submits the request will only be able to
     * see it when calling /api/loan/lend_request on their respective nodes.
     *
     * This end-point takes loanAmount, interestRate  as parameters
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @PUT
    @Path("confrim_lender")
    fun confirmLoan(@QueryParam("loan_amount") loanAmount: Int,
                  @QueryParam("interest_rate") interestRate: Int,@QueryParam ("lender") lender:CordaX500Name) : Response {

        return try {
            val lender = rpcOps.wellKnownPartyFromX500Name(lender) ?:
            return Response.status(BAD_REQUEST).entity("Party named $lender cannot be found.\n").build()

            val status= "Closed"

            val flowHandle = rpcOps.startTrackedFlow(::ConfrimLender, loanAmount,interestRate, status, listOf(lender) )
            flowHandle.progress.subscribe { println(">> $it") }

            // The line below blocks and waits for the future to resolve.
            val result = flowHandle.returnValue.getOrThrow()

            Response.status(CREATED).entity("Transaction id ${result.id} committed to ledger.\n").build()

        }catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }

    }


}